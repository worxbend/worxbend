package com.worxbend.dscrbo

import com.worxbend.dscrbo.annotations.Excluded
import com.worxbend.dscrbo.annotations.Redacted

import scala.deriving.Mirror
import scala.quoted.*

/** Expansion-time engine behind [[Describe.derived]] and [[ToString.derived]].
  *
  * Every decision that can be made from the declared types — which fields are omitted, which are redacted, how each
  * declared type is spelled, which renderer a field's type needs — is made here, once. What survives into the generated
  * code is a straight-line assembly of already-decided pieces plus the runtime [[Configuration]] reads that genuinely
  * cannot be resolved earlier.
  *
  * The work itself lives in [[Expansion]], one instance per expanded root type, so that every step is an addressable
  * member with an explicit result type rather than a local definition buried in one long method.
  */
private[dscrbo] object DescribeMacro:

  /** Resolution of a single field. First match wins, evaluated exactly once per field at expansion time:
    *   1. `@Excluded` or `@transient` -> [[FieldRule.Omit]];
    *   1. `@Redacted` -> [[FieldRule.Redact]];
    *   1. otherwise -> [[FieldRule.Render]].
    *
    * `@transient` is a serialization marker honoured only for backwards compatibility; `@Excluded` is the intended
    * spelling. Both map to `Omit` in one place, which is what makes exclusion beat redaction.
    */
  private enum FieldRule:

    case Omit
    case Redact(replacement: String)
    case Render

  /** What a bare `@Redacted` renders in place of the field's value. */
  private val DefaultReplacement: String = "<redacted>"

  /** How many types deep the macro inlines below the root before it refuses to unroll further.
    *
    * Only case classes, value classes and sealed families consume a level; `Option`, `Map`, `Array` and collection
    * wrappers do not, so the number means what a reader expects it to mean.
    */
  private val MaxNestedTypes: Int = 12

  /** How many nested layers of generated code the macro will emit before it refuses to unroll further.
    *
    * Unlike [[MaxNestedTypes]] this counts wrappers too, because a wrapper costs an emitted layer even though it is
    * not a type the user thinks of as nesting. It exists for one reason: past roughly two dozen layers the Scala 3
    * staging phase overflows a default 1 MB compiler stack, and the value is calibrated below every shape that was
    * measured to do so. A model that trips this cap is refused with a message rather than a `StackOverflowError`.
    *
    * '''This number is an empirical measurement, not a derivation.''' It was taken against one compiler on one stack
    * size, so treat it as a floor that is known safe rather than as the true limit. To re-derive it:
    *
    *   1. build a chain of `N` nested case classes, each wrapping the next in `Option[List[Map[String, _]]]`, so that
    *      every level costs four layers;
    *   1. compile it with `-Xss` set to the stack size you care about (the shipped value assumes the JVM default of
    *      1 MB — `./mill libs.commons.dscrbo.test` inherits it);
    *   1. bisect `N` for the largest value that compiles without a `StackOverflowError` in the staging phase;
    *   1. set this constant safely below the layer count that `N` implies, and record the compiler version and stack
    *      size you used here.
    *
    * Last measured against Scala 3.8.4 on a 1 MB stack. `NestingDepthSuite` pins both the accept and the refuse side,
    * so lowering this constant breaks a test rather than silently shrinking what consumers can derive.
    */
  private val MaxEmittedLayers: Int = 20

  def describeImpl[T: Type](using Quotes): Expr[Describe[T]] =
    '{
      lazy val instance: Describe[T] =
        Describe.FromFunction[T]((value: T, conf: Configuration) =>
          ${ Expansion[T]('conf, Some('instance)).renderRoot('value) }
        )
      instance
    }

  def toStringImpl[T: Type](
      value: Expr[T],
      conf: Expr[Configuration],
  )(using Quotes): Expr[String] = Expansion[T](conf, None).renderRoot(value)

  /** One expansion of one root type `T`.
    *
    * `conf` is the runtime configuration expression the generated code reads; `self` is the instance the generated code
    * can call back into, which is what makes a self-recursive root type work without unrolling forever.
    */
  final private class Expansion[T: Type](conf: Expr[Configuration], self: Option[Expr[Describe[T]]])(using
      quotes: Quotes):

    import quotes.reflect.*

    // --------------------------------------------------------------- nesting

    /** Where the expansion currently is.
      *
      * `types` counts the case classes, value classes and sealed families entered below the root: the cap a reader can
      * reason about. `layers` counts every layer of generated code, wrappers included, and exists only to keep the tree
      * handed to the staging phase shallow enough for a default compiler stack. `open` holds the types already being
      * expanded further up, which is what detects a cycle.
      */
    final private case class Nesting(types: Int, layers: Int, open: List[TypeRepr]):

      /** Descending through a wrapper: an `Option`, a collection, a map or an array. */
      def wrapped: Nesting = copy(layers = layers + 1)

      /** Descending into the fields of `tpe`. */
      def inside(tpe: TypeRepr): Nesting = Nesting(types + 1, layers + 1, tpe :: open)

      /** Descending through a dispatch on the branches of the sealed family `tpe`. */
      def dispatching(tpe: TypeRepr): Nesting = Nesting(types, layers + 1, tpe :: open)

      /** True when `tpe` is already being expanded further up, so inlining it again would not terminate. */
      def isOpen(tpe: TypeRepr): Boolean = open.exists(_ =:= tpe)

    // ---------------------------------------------------------------- symbols

    private val excludedSymbol: Symbol  = TypeRepr.of[Excluded].typeSymbol
    private val redactedSymbol: Symbol  = TypeRepr.of[Redacted].typeSymbol
    private val transientSymbol: Symbol = TypeRepr.of[scala.transient].typeSymbol

    /** Types that carry no information about what they actually hold, so nothing may be assumed about their fields. */
    private val universalTypes: List[TypeRepr] =
      List(TypeRepr.of[Any], TypeRepr.of[AnyRef], TypeRepr.of[AnyVal], TypeRepr.of[Matchable])

    // ------------------------------------------------------------------ types

    /** Dealiases and widens, but keeps singleton module types (case objects, enum values) intact. */
    private def structural(tpe: TypeRepr): TypeRepr =
      val dealiased = tpe.dealias
      if dealiased.termSymbol.exists && dealiased.termSymbol.flags.is(Flags.Module) then dealiased
      else dealiased.widen.dealias

    private def typeArgsOf(tpe: TypeRepr, className: String): Option[List[TypeRepr]] =
      val cls = Symbol.requiredClass(className)
      if !tpe.derivesFrom(cls) then None
      else
        tpe.baseType(cls) match
          case AppliedType(_, args) => Some(args)
          case _                    => None

    private def coerce[U: Type](term: Term): Expr[U] =
      // The cast is load-bearing only for `this.type` receivers, where the declared and structural types differ.
      if term.tpe <:< TypeRepr.of[U] then term.asExprOf[U]
      else TypeApply(Select.unique(term, "asInstanceOf"), List(TypeTree.of[U])).asExprOf[U]

    private def isModuleType(tpe: TypeRepr): Boolean =
      (tpe.termSymbol.exists && tpe.termSymbol.flags.is(Flags.Module)) || tpe.typeSymbol.flags.is(Flags.Module)

    private def isSealedParent(tpe: TypeRepr): Boolean =
      val symbol = tpe.typeSymbol
      (symbol.flags.is(Flags.Sealed) || symbol.flags.is(Flags.Enum)) && symbol.children.nonEmpty

    private def isCaseClass(tpe: TypeRepr): Boolean =
      tpe.typeSymbol.flags.is(Flags.Case) && !isModuleType(tpe)

    private def isValueClass(tpe: TypeRepr): Boolean =
      isCaseClass(tpe) && tpe.derivesFrom(Symbol.requiredClass("scala.AnyVal")) &&
      tpe.typeSymbol.caseFields.sizeIs == 1

    /** True when the declared type could stand for something else at runtime — a trait, an abstract class, an abstract
      * type member, `Any` and friends. Such a value may be an annotated case class, so rendering it with `toString`
      * would print a `@Redacted` field in the clear. The macro refuses instead of leaking.
      */
    private def isAbstractlyTyped(tpe: TypeRepr): Boolean =
      val symbol = tpe.typeSymbol
      symbol.isAbstractType || symbol.flags.is(Flags.Trait) || symbol.flags.is(Flags.Abstract) ||
      universalTypes.exists(tpe =:= _)

    // ------------------------------------------------------------------ names

    private def stripDollar(segment: String): String =
      if segment.endsWith("$") then segment.dropRight(1) else segment

    private def qualifiedNameOf(symbol: Symbol): String =
      symbol.fullName.split('.').iterator.map(stripDollar).mkString(".")

    private def simpleNameOf(symbol: Symbol): String = stripDollar(symbol.name)

    private def namingSymbolOf(tpe: TypeRepr): Symbol =
      if tpe.termSymbol.exists && tpe.termSymbol.flags.is(Flags.Module) then tpe.termSymbol else tpe.typeSymbol

    private def nameExpr(symbol: Symbol): Expr[String] =
      val qualified = qualifiedNameOf(symbol)
      '{
        Rendering.selectName(
          ${ Expr(simpleNameOf(symbol)) },
          ${ Expr(qualified) },
          ${ Expr(Rendering.compressPackages(qualified)) },
          $conf,
        )
      }

    // ------------------------------------------------------------ annotations

    private def stringLiteralOf(argument: Term): Option[String] =
      argument match
        case NamedArg(_, inner)             => stringLiteralOf(inner)
        case Typed(inner, _)                => stringLiteralOf(inner)
        case Inlined(_, _, inner)           => stringLiteralOf(inner)
        case Literal(StringConstant(value)) => Some(value)
        case _                              => None

    private def isDefaultArgument(argument: Term): Boolean =
      argument match
        case NamedArg(_, inner) => isDefaultArgument(inner)
        case Select(_, name)    => name.startsWith("$lessinit$greater$default$")
        case _                  => false

    private def replacementOf(annotation: Term): String =
      annotation match
        case Apply(_, Nil)           => DefaultReplacement
        case Apply(_, argument :: _) =>
          stringLiteralOf(argument) match
            case Some(literal)                       => literal
            case None if isDefaultArgument(argument) => DefaultReplacement
            case None                                =>
              report.errorAndAbort(s"@Redacted(replacement = ...) must be a string literal, found ${argument.show}.")
        case _                       => DefaultReplacement

    /** `Symbol.annotations` does not promise source order, so declaration order is recovered from positions. */
    private def redactionsOf(field: Symbol): List[Term] =
      field.annotations.filter(annotation => annotation.tpe.typeSymbol == redactedSymbol).sortBy(_.pos.start)

    private def ruleOf(field: Symbol): FieldRule =
      if field.hasAnnotation(excludedSymbol) || field.hasAnnotation(transientSymbol) then FieldRule.Omit
      else
        redactionsOf(field).headOption match
          case Some(annotation) => FieldRule.Redact(replacementOf(annotation))
          case None             => FieldRule.Render

    // ----------------------------------------------------------- value render

    private def renderPrimitive(tpe: TypeRepr, term: Term): Option[Expr[String]] =
      tpe.asType match
        case '[String]  => Some('{ Rendering.string(${ term.asExprOf[String] }) })
        case '[Char]    => Some('{ Rendering.char(${ term.asExprOf[Char] }) })
        case '[Boolean] => Some('{ ${ term.asExprOf[Boolean] }.toString })
        case '[Byte]    => Some('{ ${ term.asExprOf[Byte] }.toString })
        case '[Short]   => Some('{ ${ term.asExprOf[Short] }.toString })
        case '[Int]     => Some('{ ${ term.asExprOf[Int] }.toString })
        case '[Long]    => Some('{ ${ term.asExprOf[Long] }.toString })
        case '[Float]   => Some('{ ${ term.asExprOf[Float] }.toString })
        case '[Double]  => Some('{ ${ term.asExprOf[Double] }.toString })
        case '[Unit]    => Some('{ ${ term.asExprOf[Unit] }.toString })
        case _          => None

    /** A user-written instance always wins, so it is consulted before any structural handler. */
    private def renderSummoned(tpe: TypeRepr, term: Term): Option[Expr[String]] =
      tpe.asType match
        case '[t] =>
          Expr.summon[Describe[t]].map(instance => '{ Rendering.nested[t](${ coerce[t](term) }, $instance, $conf) })

    /** The root's own instance, used before implicit search so that deriving a recursive type is not a cyclic search. */
    private def renderSelf(tpe: TypeRepr, term: Term): Option[Expr[String]] =
      self
        .filter(_ => tpe =:= structural(TypeRepr.of[T]))
        .map(instance => '{ Rendering.nested[T](${ coerce[T](term) }, $instance, $conf) })

    private def renderOption(tpe: TypeRepr, term: Term, nesting: Nesting): Option[Expr[String]] =
      typeArgsOf(tpe, "scala.Option").collect {
        case List(element) =>
          element.asType match
            case '[e] =>
              '{
                Rendering.optionValue[e](
                  ${ coerce[Option[e]](term) },
                  (item: e) => ${ renderValue(element, 'item.asTerm, nesting.wrapped) },
                )
              }
      }

    private def renderScalaMap(tpe: TypeRepr, term: Term, nesting: Nesting): Option[Expr[String]] =
      typeArgsOf(tpe, "scala.collection.Map").collect {
        case List(keyType, valueType) =>
          keyType.asType match
            case '[k] =>
              valueType.asType match
                case '[v] =>
                  '{
                    Rendering.mapValue[k, v](
                      ${ coerce[scala.collection.Map[k, v]](term) },
                      (key: k) => ${ renderValue(keyType, 'key.asTerm, nesting.wrapped) },
                      (item: v) => ${ renderValue(valueType, 'item.asTerm, nesting.wrapped) },
                    )
                  }
      }

    private def renderArray(tpe: TypeRepr, term: Term, nesting: Nesting): Option[Expr[String]] =
      Option.when(tpe.typeSymbol == defn.ArrayClass)(tpe).collect {
        case AppliedType(_, List(element)) =>
          element.asType match
            case '[e] =>
              '{
                Rendering.arrayValue[e](
                  ${ coerce[Array[e]](term) },
                  (item: e) => ${ renderValue(element, 'item.asTerm, nesting.wrapped) },
                )
              }
      }

    private def renderScalaIterable(tpe: TypeRepr, term: Term, nesting: Nesting): Option[Expr[String]] =
      typeArgsOf(tpe, "scala.collection.Iterable").collect {
        case List(element) =>
          element.asType match
            case '[e] =>
              '{
                Rendering.iterableValue[e](
                  ${ coerce[Iterable[e]](term) },
                  (item: e) => ${ renderValue(element, 'item.asTerm, nesting.wrapped) },
                )
              }
      }

    private def renderJavaMap(tpe: TypeRepr, term: Term, nesting: Nesting): Option[Expr[String]] =
      typeArgsOf(tpe, "java.util.Map").collect {
        case List(keyType, valueType) =>
          keyType.asType match
            case '[k] =>
              valueType.asType match
                case '[v] =>
                  '{
                    Rendering.javaMapValue[k, v](
                      ${ coerce[java.util.Map[k, v]](term) },
                      (key: k) => ${ renderValue(keyType, 'key.asTerm, nesting.wrapped) },
                      (item: v) => ${ renderValue(valueType, 'item.asTerm, nesting.wrapped) },
                    )
                  }
      }

    private def renderJavaIterable(tpe: TypeRepr, term: Term, nesting: Nesting): Option[Expr[String]] =
      typeArgsOf(tpe, "java.lang.Iterable").collect {
        case List(element) =>
          element.asType match
            case '[e] =>
              '{
                Rendering.javaIterableValue[e](
                  ${ coerce[java.lang.Iterable[e]](term) },
                  (item: e) => ${ renderValue(element, 'item.asTerm, nesting.wrapped) },
                )
              }
      }

    /** Reached only once the root instance and implicit search have both declined, so the advice is actionable. */
    private def renderCycle(tpe: TypeRepr, nesting: Nesting): Option[Expr[String]] =
      Option.when(nesting.isOpen(tpe))(
        report.errorAndAbort(
          s"Describe cannot inline the recursive type ${tpe.show}: add `derives Describe` to it, " +
            s"or provide a `given Describe[${tpe.show}]` in scope."
        )
      )

    /** Last resort. Safe only for a type that cannot stand for an annotated subtype at runtime. */
    private def renderOpaque(tpe: TypeRepr, term: Term): Expr[String] =
      if !isAbstractlyTyped(tpe) then '{ Rendering.opaque(${ term.asExprOf[Any] }) }
      else
        report.errorAndAbort(
          s"Describe cannot see into ${tpe.show}: the declared type is abstract, so the runtime value may be a type " +
            "with @Redacted fields and rendering it with toString would print them in the clear. Provide a " +
            s"`given Describe[${tpe.show}]`, seal the hierarchy, or mark the field @Excluded."
        )

    private def renderStructure(tpe: TypeRepr, term: Term, nesting: Nesting): Option[Expr[String]] =
      if isModuleType(tpe) then Some(nameExpr(namingSymbolOf(tpe)))
      else if isSealedParent(tpe) then Some(withinBudget(tpe, nesting)(renderSealed(tpe, term, nesting)))
      else if isValueClass(tpe) then Some(withinBudget(tpe, nesting)(renderValueClass(tpe, term, nesting)))
      else if isCaseClass(tpe) then Some(withinBudget(tpe, nesting)(renderProduct(tpe, term, nesting)))
      else None

    /** Both caps name the type that is actually too deep, and prescribe a remedy implicit search really does honour. */
    private def withinBudget(tpe: TypeRepr, nesting: Nesting)(rendered: => Expr[String]): Expr[String] =
      if nesting.types > MaxNestedTypes then report.errorAndAbort(tooDeep(tpe, s"$MaxNestedTypes levels of nesting"))
      else if nesting.layers > MaxEmittedLayers then
        report.errorAndAbort(
          tooDeep(
            tpe,
            s"$MaxEmittedLayers layers of generated code (every Option, collection, map and array wrapper costs a " +
              "layer, and this many would risk exhausting the compiler stack in a later phase)",
          )
        )
      else rendered

    private def tooDeep(tpe: TypeRepr, budget: String): String =
      s"Describe reached $budget at ${tpe.show} and stopped inlining. Add `derives Describe` to ${tpe.show}, or " +
        s"provide a `given Describe[${tpe.show}]` in scope, so that the chain is broken by a call to an instance " +
        "instead of being unrolled any further."

    private def renderValue(rawType: TypeRepr, term: Term, nesting: Nesting): Expr[String] =
      val tpe = structural(rawType)
      renderPrimitive(tpe, term)
        .orElse(renderSelf(tpe, term))
        .orElse(renderSummoned(tpe, term))
        .orElse(renderOption(tpe, term, nesting))
        .orElse(renderScalaMap(tpe, term, nesting))
        .orElse(renderArray(tpe, term, nesting))
        .orElse(renderScalaIterable(tpe, term, nesting))
        .orElse(renderJavaMap(tpe, term, nesting))
        .orElse(renderJavaIterable(tpe, term, nesting))
        .orElse(renderCycle(tpe, nesting))
        .orElse(renderStructure(tpe, term, nesting))
        .getOrElse(renderOpaque(tpe, term))

    // ---------------------------------------------------------- product shape

    private def renderValueClass(tpe: TypeRepr, term: Term, nesting: Nesting): Expr[String] =
      val parameter = tpe.typeSymbol.caseFields.head
      ruleOf(parameter) match
        // Omit on the sole parameter of a value class is ignored: there would be nothing left to print.
        case FieldRule.Redact(replacement)     => Expr(replacement)
        case FieldRule.Omit | FieldRule.Render =>
          renderValue(tpe.memberType(parameter), Select(term, parameter), nesting.inside(tpe))

    private def fieldExpr(owner: TypeRepr, field: Symbol, value: Expr[String]): Expr[String] =
      val declared  = structural(owner.memberType(field))
      val symbol    = namingSymbolOf(declared)
      val qualified = qualifiedNameOf(symbol)
      '{
        Rendering.field(
          ${ Expr(field.name) },
          ${ Expr(simpleNameOf(symbol)) },
          ${ Expr(qualified) },
          ${ Expr(Rendering.compressPackages(qualified)) },
          $value,
          $conf,
        )
      }

    private def productBody(tpe: TypeRepr, term: Term, nesting: Nesting): Expr[String] =
      val nextNesting = nesting.inside(tpe)
      val rendered    =
        tpe.typeSymbol.caseFields.flatMap { field =>
          ruleOf(field) match
            case FieldRule.Omit                => None
            case FieldRule.Redact(replacement) => Some(fieldExpr(tpe, field, Expr(replacement)))
            case FieldRule.Render              =>
              val value = renderValue(tpe.memberType(field), Select(term, field), nextNesting)
              Some(fieldExpr(tpe, field, value))
        }
      '{ Rendering.assemble(${ nameExpr(tpe.typeSymbol) }, ${ Expr.ofList(rendered) }, $conf) }

    private def renderProduct(tpe: TypeRepr, term: Term, nesting: Nesting): Expr[String] =
      tpe.asType match
        case '[t] =>
          '{
            Rendering.nullOr[t](
              ${ coerce[t](term) },
              (bound: t) => ${ productBody(tpe, 'bound.asTerm, nesting) },
            )
          }

    // ----------------------------------------------------------- sealed shape

    /** The class type parameters of a child, recovered from its primary constructor's leading clause. */
    private def typeParametersOf(child: Symbol): List[Symbol] =
      if !child.isClassDef || !child.primaryConstructor.exists then Nil
      else child.primaryConstructor.paramSymss.headOption.filter(_.exists(_.isTypeParam)).getOrElse(Nil)

    private def tupleElements(tpe: TypeRepr): List[TypeRepr] =
      tpe.asType match
        case '[head *: tail] => TypeRepr.of[head] :: tupleElements(TypeRepr.of[tail])
        case _               => Nil

    /** The children of a sealed parent, already instantiated at the parent's type arguments.
      *
      * The compiler answers this question for itself when it synthesises a `Mirror.SumOf`, so that is what is asked.
      * The mirror is read at expansion time and discarded; nothing about it reaches the generated code.
      */
    private def sumElementTypes(parent: TypeRepr): List[TypeRepr] =
      parent.asType match
        case '[p] =>
          Expr.summon[Mirror.SumOf[p]] match
            case Some('{ $_ : Mirror.SumOf[`p`] { type MirroredElemTypes = elements } }) =>
              tupleElements(TypeRepr.of[elements])
            case _                                                                       => Nil

    /** Fallback instantiation: match the child's view of its parent against the parent's actual type arguments. */
    private def unifyChild(parent: TypeRepr, child: Symbol, parameters: List[Symbol]): Option[TypeRepr] =
      (child.typeRef.baseType(parent.typeSymbol), parent) match
        case (AppliedType(_, declared), AppliedType(_, actual)) if declared.sizeIs == actual.size =>
          val bindings =
            declared.zip(actual).collect {
              case (slot, argument) if slot.typeSymbol.isTypeParam => slot.typeSymbol -> argument
            }.toMap
          val resolved = parameters.map(bindings.get)
          Option.when(resolved.forall(_.isDefined))(child.typeRef.appliedTo(resolved.flatten))
        case _                                                                                    => None

    /** The child as it exists inside `parent`. Never the raw `child.typeRef`: that drops the type arguments and makes
      * the compiler fail inside the inliner instead of here.
      */
    private def appliedChildType(parent: TypeRepr, child: Symbol): TypeRepr =
      val parameters = typeParametersOf(child)
      if parameters.isEmpty then child.typeRef
      else
        sumElementTypes(parent)
          .find(_.typeSymbol == child)
          .orElse(unifyChild(parent, child, parameters))
          .getOrElse(
            report.errorAndAbort(
              s"Describe cannot work out the type arguments of ${child.fullName} inside ${parent.show}. Provide a " +
                s"`given Describe[${parent.show}]` in scope so that the family is rendered by an instance instead."
            )
          )

    /** The child with wildcard type arguments, so the emitted type test is checkable and warning free. */
    private def erasedChildType(child: Symbol): TypeRepr =
      typeParametersOf(child) match
        case Nil        => child.typeRef
        case parameters => AppliedType(child.typeRef, parameters.map(_ => TypeBounds.empty))

    private def moduleOf(child: Symbol): Option[Symbol] =
      if child.isTerm then Some(child)
      else if child.flags.is(Flags.Module) then Option(child.companionModule).filter(_.exists)
      else None

    private def branchCondition(child: Symbol, scrutinee: Expr[Any]): Expr[Boolean] =
      moduleOf(child) match
        case Some(module) => '{ $scrutinee == ${ Ref(module).asExpr } }
        case None         =>
          erasedChildType(child).asType match
            case '[c] => '{ $scrutinee.isInstanceOf[c] }

    private def renderChild(
        parent: TypeRepr,
        child: Symbol,
        scrutinee: Expr[Any],
        nesting: Nesting,
    ): Expr[String] =
      moduleOf(child) match
        case Some(module) => nameExpr(module)
        case None         =>
          val applied = appliedChildType(parent, child)
          applied.asType match
            case '[c] => renderValue(applied, '{ $scrutinee.asInstanceOf[c] }.asTerm, nesting)

    private def renderBranches(
        parent: TypeRepr,
        children: List[Symbol],
        scrutinee: Expr[Any],
        nesting: Nesting,
    ): Expr[String] =
      children match
        case Nil           => '{ Rendering.opaque($scrutinee) }
        case child :: rest =>
          '{
            if ${ branchCondition(child, scrutinee) } then ${ renderChild(parent, child, scrutinee, nesting) }
            else ${ renderBranches(parent, rest, scrutinee, nesting) }
          }

    private def renderSealed(tpe: TypeRepr, term: Term, nesting: Nesting): Expr[String] =
      val children    = tpe.typeSymbol.children
      val nextNesting = nesting.dispatching(tpe)
      '{
        Rendering.nullOr[Any](
          ${ term.asExprOf[Any] },
          (bound: Any) => ${ renderBranches(tpe, children, 'bound, nextNesting) },
        )
      }

    // ------------------------------------------------------------------- root

    def renderRoot(root: Expr[T]): Expr[String] =
      val rootType = structural(TypeRepr.of[T])
      renderStructure(rootType, root.asTerm, Nesting(0, 0, Nil))
        .getOrElse(
          report.errorAndAbort(
            s"${rootType.show} is not a case class, case object, sealed trait or enum, " +
              "so Describe cannot be derived for it."
          )
        )
