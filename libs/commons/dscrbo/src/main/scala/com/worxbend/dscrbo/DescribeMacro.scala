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
    * not a type the user thinks of as nesting. It exists for one reason: a deep enough tree makes the Scala 3 staging
    * phase overflow the compiler's stack, and a model that trips this cap is refused with a message rather than with
    * a `StackOverflowError`.
    *
    * '''Both numbers are empirical measurements against one compiler at one stack size, and the stack size is the
    * whole story.''' Measured with Scala 3.8.4, with both caps raised out of the way so that the stack is what fails:
    *
    *   - at `-Xss1m`: a chain of case classes each wrapping the next in `Option[List[Map[String, _]]]` (four layers
    *     per level) survives 28 layers and overflows by 32; a plain chain survives 12 types below the root and
    *     overflows at 13.
    *   - at `-Xss10m`, which is what this repository actually builds with (`.mill-jvm-opts`), a plain chain of 140
    *     types and a wrapper chain of 160 layers both compile without trouble.
    *
    * So the two caps are not equally tight. Against a 1 MB stack [[MaxNestedTypes]] has '''no margin at all''' — the
    * first shape it refuses is the first shape that overflows — while `MaxEmittedLayers` keeps roughly eight layers
    * in hand. Against the 10 MB the build uses, both are about an order of magnitude conservative. Treat them as
    * calibrated for the small-stack case, which is the one that fails destructively.
    *
    * To re-derive: raise both constants far out of the way, generate the chain you care about, compile with the
    * `-Xss` you want to support, bisect for the largest shape that compiles, and record the compiler version and
    * stack size here. `NestingDepthSuite` pins both the accept and the refuse side, so changing either constant
    * breaks a test rather than silently moving what consumers can derive.
    *
    * '''Neither cap is `-Xmax-inlines`, and neither shadows it.''' That compiler setting limits *successive inline
    * expansions*, and this macro never approaches it: `Describe.derived` is a single `inline def` whose body is a
    * single splice, so the compiler's counter never rises above one however deep the model. All the recursion here is
    * ordinary recursion in [[Expansion.renderValue]], running at staging level 0, which the compiler does not count
    * and cannot bound — which is precisely why these two constants have to exist.
    *
    * Both caps bound *depth*. Neither bounds the *size* of what is emitted, which grows with the branching factor as
    * well, so a model that is wide as well as deep can be very expensive to compile while sitting inside both caps.
    * If that ever bites, the fix is a third budget counting emitted nodes, not a smaller value here.
    */
  private val MaxEmittedLayers: Int = 20

  /** How deep the redaction-reachability scan follows a type graph before it gives up and refuses.
    *
    * The scan cuts cycles by remembering the types already on the path, which is enough for any shape whose type
    * graph is finite. It is not enough for a type whose arguments grow at every step:
    *
    * {{{
    * final case class Growth[A](next: Option[Growth[List[A]]])
    * // Growth[Int] -> Growth[List[Int]] -> Growth[List[List[Int]]] -> ...
    * }}}
    *
    * No two of those are `=:=`, so there is no cycle to detect and the walk never terminates — it hangs the compiler
    * rather than failing it. This bound is what makes the scan total. It is deliberately far above any real model:
    * the deepest shape in this repository's own tests reaches 16.
    */
  private val MaxScanDepth: Int = 64

  def describeImpl[T: Type](using Quotes): Expr[Describe[T]] =
    '{
      lazy val instance: Describe[T] =
        Describe.FromFunction[T]((value: T, conf: Configuration) =>
          ${ Expansion[T]('conf, Some('instance)).renderRoot('value) }
        )
      instance
    }

  /** [[ToString.derived]] takes its configuration as an `inline` parameter, and an inline parameter creates no
    * binding: the argument tree is substituted at every occurrence. The expansion reads the configuration roughly
    * twice per rendered field at every level of nesting, so splicing `conf` directly would re-evaluate the caller's
    * expression once per occurrence — measured at 30 evaluations for a fifteen-node model. Binding it to a `val`
    * first makes it one evaluation whose result every occurrence shares, which also guarantees that a single render
    * cannot mix two different configurations. [[describeImpl]] needs no equivalent: its `conf` is a lambda parameter
    * and is therefore already a stable local.
    *
    * Two further consequences, observable only for a configuration expression that does something. The binding is
    * evaluated before the value expression, where previously the value came first; and it is evaluated
    * unconditionally, where previously a `null` root short-circuited inside `Rendering.nullOr` without ever reading
    * the configuration — so a configuration that throws now propagates on a `null` root instead of rendering `null`.
    * Both are accepted deliberately: an effectful configuration is already outside what this API promises, and one
    * evaluation with a predictable order is a better contract than N with none.
    */
  def toStringImpl[T: Type](
      value: Expr[T],
      conf: Expr[Configuration],
  )(using Quotes): Expr[String] =
    '{
      val configuration: Configuration = $conf
      ${ Expansion[T]('configuration, None).renderRoot(value) }
    }

  /** One expansion of one root type `T`.
    *
    * `conf` is the runtime configuration expression the generated code reads; `self` is the instance the generated code
    * can call back into, which is what makes a self-recursive root type work without unrolling forever.
    *
    * '''On capturing `Quotes`.''' Taking `Quotes` as a `using` class parameter and importing `quotes.reflect.*` once
    * makes it a field, which the reflection guide explicitly advises against: every `TypeRepr`, `Term` and `Symbol` in
    * the ~30 members below is then path-dependent on that one field. It is done on purpose, and the trade is worth
    * naming so that nobody either "fixes" it blindly or copies it without knowing the cost.
    *
    *   - What it buys: one fixed reflection context for the whole expansion, so each step of the derivation is an
    *     addressable member with an explicit result type, and the state every step needs — `conf`, `self`, the cached
    *     symbols — is held once rather than threaded through every signature. It also disposes of the *other* rule in
    *     the same guide, "avoid nested contexts": `quotes.Nested` appears nowhere, and every `Expr` a deeper level
    *     needs is passed to it explicitly.
    *   - What it costs: a member that receives an `Expr` created inside a splice and re-embeds it in a quote built
    *     from the captured field extrudes that `Expr` from its scope. That is not hypothetical — it is exactly what
    *     the sealed-dispatch path did until [[branchCondition]], [[renderChild]] and [[renderBranches]] were given
    *     their own `(using Quotes)`; see the note there.
    *   - What keeps it honest: `-Xcheck-macros`, enabled on this module's test module, where the macro actually
    *     expands. It should stay on — but note the guard is partial, not total. It catches an extruded `Expr`, which
    *     is why the sealed path is now clean; it does not catch one laundered through `.asTerm`, which is the shape
    *     the product path uses. A regression on the product side would compile silently.
    */
  final private class Expansion[T: Type](conf: Expr[Configuration], self: Option[Expr[Describe[T]]])(using
      quotes: Quotes):

    import quotes.reflect.*

    /** What the redaction-reachability scan concluded about a type it is about to hand to `toString`.
      *
      * `Unprovable` is not the same as `Clean`: it means the walk hit [[MaxScanDepth]] without deciding, and for a
      * library whose job is to not print secrets that has to be treated as a refusal rather than an all-clear.
      */
    private enum Reachability:

      case Clean
      case Annotated(owner: TypeRepr, field: Symbol)
      case Unprovable(at: TypeRepr)

    // --------------------------------------------------------------- nesting

    /** Where the expansion currently is.
      *
      * `types` counts the case classes, value classes and sealed families entered below the root: the cap a reader can
      * reason about. `layers` counts every layer of generated code, wrappers included, and exists only to keep the tree
      * handed to the staging phase shallow enough for a default compiler stack. `open` holds the types already being
      * expanded further up, which is what detects a cycle.
      *
      * `branch` says the expansion is positioned on the children of a sealed family. It is what distinguishes the two
      * kinds of case class this macro treats differently: a sealed branch is expanded structurally wherever it occurs,
      * because it has no instance of its own to delegate to, while an ordinary nested case class is not expanded at
      * all. Depth alone cannot tell them apart — a sealed family reached through a field is already one type deep, and
      * its branches must still expand.
      */
    final private case class Nesting(types: Int, layers: Int, open: List[TypeRepr], branch: Boolean):

      /** Descending through a wrapper: an `Option`, a collection, a map or an array. */
      def wrapped: Nesting = copy(layers = layers + 1, branch = false)

      /** Descending into the fields of `tpe`. */
      def inside(tpe: TypeRepr): Nesting = Nesting(types + 1, layers + 1, tpe :: open, branch = false)

      /** Descending through a dispatch on the branches of the sealed family `tpe`. */
      def dispatching(tpe: TypeRepr): Nesting = Nesting(types, layers + 1, tpe :: open, branch = true)

      /** True when `tpe` is already being expanded further up, so inlining it again would not terminate. */
      def isOpen(tpe: TypeRepr): Boolean = open.exists(_ =:= tpe)

    // ---------------------------------------------------------------- symbols

    private val excludedSymbol: Symbol  = TypeRepr.of[Excluded].typeSymbol
    private val redactedSymbol: Symbol  = TypeRepr.of[Redacted].typeSymbol
    private val transientSymbol: Symbol = TypeRepr.of[scala.transient].typeSymbol

    /** The container classes [[renderValue]] dispatches on, resolved once through `TypeRepr.of[_].typeSymbol` rather
      * than looked up by name on every call. Naming the class in Scala rather than in a string is what the macro
      * guides prescribe, and it makes a typo a compile error here instead of an expansion-time crash at a use site.
      */
    private val optionClass: Symbol        = TypeRepr.of[Option[Any]].typeSymbol
    private val scalaMapClass: Symbol      = TypeRepr.of[scala.collection.Map[Any, Any]].typeSymbol
    private val scalaIterableClass: Symbol = TypeRepr.of[scala.collection.Iterable[Any]].typeSymbol
    private val javaMapClass: Symbol       = TypeRepr.of[java.util.Map[Any, Any]].typeSymbol
    private val javaIterableClass: Symbol  = TypeRepr.of[java.lang.Iterable[Any]].typeSymbol

    /** Types that carry no information about what they actually hold, so nothing may be assumed about their fields. */
    private val universalTypes: List[TypeRepr] =
      List(TypeRepr.of[Any], TypeRepr.of[AnyRef], TypeRepr.of[AnyVal], TypeRepr.of[Matchable])

    // ------------------------------------------------------------------ types

    /** The root type as every comparison against it needs to see it. Computed once: [[renderSelf]] consults it for
      * every value rendered anywhere in the expansion, not just at the root.
      */
    private val rootType: TypeRepr = structural(TypeRepr.of[T])

    /** Dealiases and widens, but keeps singleton module types (case objects, enum values) intact. */
    private def structural(tpe: TypeRepr): TypeRepr =
      val dealiased = tpe.dealias
      if dealiased.termSymbol.exists && dealiased.termSymbol.flags.is(Flags.Module) then dealiased
      else dealiased.widen.dealias

    private def typeArgsOf(tpe: TypeRepr, cls: Symbol): Option[List[TypeRepr]] =
      if !tpe.derivesFrom(cls) then None
      else
        tpe.baseType(cls) match
          case AppliedType(_, args) => Some(args)
          case _                    => None

    private def coerce[U: Type](term: Term): Expr[U] =
      // The cast is load-bearing only for `this.type` receivers, where the declared and structural types differ.
      // Written as a quote rather than a hand-built `TypeApply`, so the typer checks it instead of the author.
      if term.tpe <:< TypeRepr.of[U] then term.asExprOf[U]
      else '{ ${ term.asExprOf[Any] }.asInstanceOf[U] }

    private def isModuleType(tpe: TypeRepr): Boolean =
      (tpe.termSymbol.exists && tpe.termSymbol.flags.is(Flags.Module)) || tpe.typeSymbol.flags.is(Flags.Module)

    private def isSealedParent(tpe: TypeRepr): Boolean =
      val symbol = tpe.typeSymbol
      (symbol.flags.is(Flags.Sealed) || symbol.flags.is(Flags.Enum)) && symbol.children.nonEmpty

    private def isCaseClass(tpe: TypeRepr): Boolean =
      tpe.typeSymbol.flags.is(Flags.Case) && !isModuleType(tpe)

    private def isValueClass(tpe: TypeRepr): Boolean =
      isCaseClass(tpe) && tpe.derivesFrom(defn.AnyValClass) &&
      tpe.typeSymbol.caseFields.sizeIs == 1

    /** True when the declared type could stand for something else at runtime — a trait, an abstract class, an abstract
      * type member, `Any` and friends. Such a value may be an annotated case class, so rendering it with `toString`
      * would print a `@Redacted` field in the clear. The macro refuses instead of leaking.
      *
      * The three composite shapes are matched before the flag tests rather than after, because for all three the flag
      * tests are vacuously false and would wave the type through:
      *
      *   - an intersection and a union both have `NoSymbol` for a `typeSymbol`, so every flag test answers `false` on
      *     a symbol that does not exist. Neither shape pins down a runtime class, so both are refused outright.
      *   - a refinement's `typeSymbol` is the *alias* symbol of the refined type, which carries none of the parent's
      *     flags — `AnyRef { def foo: Int }` is neither abstract nor `=:= AnyRef`. Refining a type adds members but
      *     cannot narrow which classes inhabit it, so the question is answered by the parent, recursively.
      *
      * '''Known hole, deliberately still open: a concrete but non-final class.''' Nothing here tests `Flags.Final`,
      * so a field declared at `class Base` accepts a `final case class Sub(@Redacted secret: String) extends Base`
      * and prints the secret in the clear. Closing it means refusing anything but a final class, which also refuses
      * `java.lang.Throwable` and most of the Java library — `OpaqueTypeSuite` pins `Throwable` as renderable, and
      * neither module's README says which way this should go. It is a specification decision, not an oversight; do
      * not close it here without settling that first.
      */
    private def isAbstractlyTyped(tpe: TypeRepr): Boolean =
      tpe match
        case AndType(_, _) | OrType(_, _) => true
        case Refinement(parent, _, _)     => isAbstractlyTyped(parent)
        case _                            =>
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

    /** All three spellings are computed here, at expansion time, and reach the generated code as string literals; only
      * the choice between them is left to the runtime `Configuration`.
      */
    private def nameExpr(symbol: Symbol): Expr[String] =
      val qualified  = qualifiedNameOf(symbol)
      val simple     = Expr(simpleNameOf(symbol))
      val full       = Expr(qualified)
      val compressed = Expr(Rendering.compressPackages(qualified))
      '{ Rendering.selectName($simple, $full, $compressed, $conf) }

    // ------------------------------------------------------------ annotations

    /** Deliberately narrower than `argument.asExpr.value`, and not to be "simplified" onto it.
      *
      * `FromExpr[String]` — what `.value` uses — also constant-folds references to `inline val` and `final val`, so
      * delegating to it would silently start accepting `@Redacted(SomeConstant)`. That contradicts the documented
      * contract on `@Redacted` ("replacement must be a string literal") and would widen what the annotation accepts
      * with no test pinning either behaviour. The literal shapes below are matched by hand so that the boundary stays
      * exactly where the annotation's own documentation puts it. The `NamedArg` strip is outside `FromExpr`'s remit
      * in any case and would be needed either way.
      */
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

    /** The ten arms below look like duplication asking to be folded into a single `case '[t] if isPrimitive(tpe)`, and
      * they must not be. The *static* type of the spliced term is what selects the primitive `toString`: written as
      * `term.asExprOf[Int]` the compiler emits the unboxed `Int.toString`, whereas a bound type variable `t` would
      * resolve to `Any.toString` and box every primitive field of every rendered value. The repetition is
      * load-bearing.
      *
      * This is also the one structural handler consulted ahead of [[renderSelf]] and [[renderSummoned]] — see the note
      * on [[renderSummoned]] for why primitives are deliberately not overridable.
      */
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

    /** A user-written instance beats every structural handler except [[renderPrimitive]], which [[renderValue]]
      * consults first. The ten primitive types are therefore resolved statically and are *not* overridable: a
      * `given Describe[Int]` in scope is silently ignored for `Int` fields. That is deliberate — it is what lets this
      * module ship no per-type instances at all, and it saves an implicit search per primitive field at expansion
      * time. `KnownDivergenceSuite` records the same design choice from the other direction.
      */
    private def renderSummoned(tpe: TypeRepr, term: Term): Option[Expr[String]] =
      tpe.asType match
        case '[t] =>
          Expr.summon[Describe[t]].map { instance =>
            val value = coerce[t](term)
            '{ Rendering.nested[t]($value, $instance, $conf) }
          }

    /** The root's own instance, used before implicit search so that deriving a recursive type is not a cyclic search. */
    private def renderSelf(tpe: TypeRepr, term: Term): Option[Expr[String]] =
      self
        .filter(_ => tpe =:= rootType)
        .map { instance =>
          val value = coerce[T](term)
          '{ Rendering.nested[T]($value, $instance, $conf) }
        }

    private def renderOption(tpe: TypeRepr, term: Term, nesting: Nesting): Option[Expr[String]] =
      typeArgsOf(tpe, optionClass).collect {
        case List(element) =>
          element.asType match
            case '[e] =>
              val source = coerce[Option[e]](term)
              '{
                Rendering.optionValue[e](
                  $source,
                  (item: e) => ${ renderValue(element, 'item.asTerm, nesting.wrapped) },
                )
              }
      }

    private def renderScalaMap(tpe: TypeRepr, term: Term, nesting: Nesting): Option[Expr[String]] =
      typeArgsOf(tpe, scalaMapClass).collect {
        case List(keyType, valueType) =>
          keyType.asType match
            case '[k] =>
              valueType.asType match
                case '[v] =>
                  val source = coerce[scala.collection.Map[k, v]](term)
                  '{
                    Rendering.mapValue[k, v](
                      $source,
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
              val source = coerce[Array[e]](term)
              '{
                Rendering.arrayValue[e](
                  $source,
                  (item: e) => ${ renderValue(element, 'item.asTerm, nesting.wrapped) },
                )
              }
      }

    private def renderScalaIterable(tpe: TypeRepr, term: Term, nesting: Nesting): Option[Expr[String]] =
      typeArgsOf(tpe, scalaIterableClass).collect {
        case List(element) =>
          element.asType match
            case '[e] =>
              val source = coerce[Iterable[e]](term)
              '{
                Rendering.iterableValue[e](
                  $source,
                  (item: e) => ${ renderValue(element, 'item.asTerm, nesting.wrapped) },
                )
              }
      }

    private def renderJavaMap(tpe: TypeRepr, term: Term, nesting: Nesting): Option[Expr[String]] =
      typeArgsOf(tpe, javaMapClass).collect {
        case List(keyType, valueType) =>
          keyType.asType match
            case '[k] =>
              valueType.asType match
                case '[v] =>
                  val source = coerce[java.util.Map[k, v]](term)
                  '{
                    Rendering.javaMapValue[k, v](
                      $source,
                      (key: k) => ${ renderValue(keyType, 'key.asTerm, nesting.wrapped) },
                      (item: v) => ${ renderValue(valueType, 'item.asTerm, nesting.wrapped) },
                    )
                  }
      }

    private def renderJavaIterable(tpe: TypeRepr, term: Term, nesting: Nesting): Option[Expr[String]] =
      typeArgsOf(tpe, javaIterableClass).collect {
        case List(element) =>
          element.asType match
            case '[e] =>
              val source = coerce[java.lang.Iterable[e]](term)
              '{
                Rendering.javaIterableValue[e](
                  $source,
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
      if !isAbstractlyTyped(tpe) then
        val value = term.asExprOf[Any]
        '{ Rendering.opaque($value) }
      else
        report.errorAndAbort(
          s"Describe cannot see into ${tpe.show}: the declared type is abstract, so the runtime value may be a type " +
            "with @Redacted fields and rendering it with toString would print them in the clear. Provide a " +
            s"`given Describe[${tpe.show}]`, seal the hierarchy, or mark the field @Excluded."
        )

    /** Which shapes this macro expands structurally, and which it delegates.
      *
      * '''A nested case class is never unrolled into its parent.''' A case class is expanded in exactly two
      * positions: at the root, the type the user actually wrote `derives Describe` on; and as a branch of a sealed
      * family being dispatched, which has no instance of its own to delegate to. Anywhere else, a case-class-typed
      * field has
      * already been offered to [[renderSelf]] and [[renderSummoned]], so reaching here means it has no instance of its
      * own, and the answer is `toString` via [[renderOpaque]] rather than another level of inlining.
      *
      * This is the composition story a typeclass is supposed to have: a nested type earns structured rendering by
      * carrying its own `derives Describe`. Unrolling it instead made one derivation's emitted expression grow with
      * the whole reachable object graph, which is what forced [[MaxNestedTypes]], [[MaxEmittedLayers]] and a cycle
      * stack into existence, and what put wide-and-deep models within reach of the JVM's 65,535-byte per-method
      * `Code` limit. Delegating measured 1.51x faster and 13.5x smaller at depth 12.
      *
      * Enums, sealed families, case objects and value classes keep their structural treatment: for those, inlining is
      * the point — a sealed branch has no instance of its own to delegate to, and a value class exists precisely to
      * be seen through.
      */
    private def renderStructure(tpe: TypeRepr, term: Term, nesting: Nesting): Option[Expr[String]] =
      if isModuleType(tpe) then Some(nameExpr(namingSymbolOf(tpe)))
      else if isSealedParent(tpe) then Some(withinBudget(tpe, nesting)(renderSealed(tpe, term, nesting)))
      else if isValueClass(tpe) then Some(withinBudget(tpe, nesting)(renderValueClass(tpe, term, nesting)))
      else if isCaseClass(tpe) && (nesting.types == 0 || nesting.branch) then
        Some(withinBudget(tpe, nesting)(renderProduct(tpe, term, nesting)))
      else if isCaseClass(tpe) then refuseIfItRedacts(tpe)
      else None

    /** The one case where falling back to `toString` would betray the library's purpose.
      *
      * A nested case class without an instance renders with its own `toString`. That is the intended design — but
      * `toString` ignores `@Redacted` and `@Excluded` entirely, and it does so at *every* depth below the field, not
      * just on the type named there. An unannotated intermediate is therefore not safe:
      *
      * {{{
      * final case class Secret(@Redacted token: String)
      * final case class Middle(s: Secret)            // declares nothing itself
      * final case class Outer(m: Middle) derives Describe
      * // Outer(m = Middle(Secret(hunter2)))         <- the secret, in the clear
      * }}}
      *
      * So the scan follows the whole reachable shape rather than the immediate field list. The annotations are
      * visible at expansion time without expanding anything, so the mistake costs a compile error rather than a
      * silent leak, and the error names the type and field that actually carry the annotation.
      *
      * Returning `None` for everything else is what lets [[renderOpaque]] answer.
      */
    private def refuseIfItRedacts(tpe: TypeRepr): Option[Expr[String]] =
      annotatedWithin(tpe, Nil, 0) match
        case Reachability.Clean => None

        case Reachability.Annotated(owner, field) =>
          val where =
            if owner =:= tpe then s"it declares `${field.name}`"
            else s"${owner.show}, reachable from it, declares `${field.name}`"
          Some(
            report.errorAndAbort(
              s"Describe will not render ${tpe.show} with toString: $where as @Redacted or @Excluded, and toString " +
                s"ignores both at every depth, so the value would be printed in the clear. Nested case classes are " +
                s"not unrolled into their parent, so add `derives Describe` to ${tpe.show}, provide a " +
                s"`given Describe[${tpe.show}]`, or mark the field @Excluded here."
            )
          )

        case Reachability.Unprovable(at) =>
          Some(
            report.errorAndAbort(
              s"Describe cannot prove that rendering ${tpe.show} with toString would not print a @Redacted field: " +
                s"its type graph is still growing at ${at.show} after $MaxScanDepth levels, which happens when a " +
                "recursive type applies a wrapper to its own parameter. Rather than guess, it refuses. Add " +
                s"`derives Describe` to ${tpe.show}, provide a `given Describe[${tpe.show}]`, or mark the field " +
                "@Excluded here."
            )
          )

    /** The first `@Redacted` or `@Excluded` field reachable from `tpe`, with the type that declares it.
      *
      * Reachability follows case-class fields, the type arguments of applied types — so a `List[Secret]` is caught —
      * and the branches of sealed families. `seen` makes an ordinary recursive model terminate; a type already on the
      * path cannot introduce an annotation that has not been examined at its first occurrence. `depth` handles the
      * case `seen` cannot: a type whose arguments grow at every step never repeats, so there is no cycle to detect
      * and the walk would otherwise hang the compiler. See [[MaxScanDepth]].
      *
      * Note that a reachable type having its own `Describe` instance does not make it safe here. Once the outermost
      * type is rendered by `toString`, every instance below it is bypassed too.
      */
    private def annotatedWithin(tpe: TypeRepr, seen: List[TypeRepr], depth: Int): Reachability =
      if seen.exists(_ =:= tpe) then Reachability.Clean
      else if depth > MaxScanDepth then Reachability.Unprovable(tpe)
      else
        val declaredHere =
          if isCaseClass(tpe) || isModuleType(tpe) then
            tpe.typeSymbol.caseFields.find(field => ruleOf(field) != FieldRule.Render).map(field => (tpe, field))
          else None
        declaredHere match
          case Some((owner, field)) => Reachability.Annotated(owner, field)
          case None                 =>
            val next = tpe :: seen
            // Keeps the first non-Clean answer and stops recursing once one is found.
            reachableFrom(tpe).foldLeft(Reachability.Clean: Reachability):
              case (Reachability.Clean, candidate) => annotatedWithin(candidate, next, depth + 1)
              case (decided, _)                    => decided

    /** Types whose annotations `toString` on `tpe` would also expose: its fields, its type arguments, its branches. */
    private def reachableFrom(tpe: TypeRepr): List[TypeRepr] =
      val arguments = tpe match
        case AppliedType(_, args) => args
        case _                    => Nil
      val fields    =
        if isCaseClass(tpe) then tpe.typeSymbol.caseFields.map(field => tpe.memberType(field))
        else Nil
      // The uninstantiated child reference is enough: only annotations are read here, and those do not depend on
      // how the family's type parameters happen to be applied.
      val branches  =
        if isSealedParent(tpe) then tpe.typeSymbol.children.map(_.typeRef)
        else Nil
      // Abstract types are deliberately NOT filtered out here. A `List[Secret]` field and a sealed-trait field are
      // both abstract at the top, and skipping them would hide exactly the descendants this scan exists to find —
      // their type arguments and branches are the interesting part. They contribute no `caseFields` of their own, so
      // including them only widens reachability; `seen` is what makes it terminate.
      (arguments ++ fields ++ branches).map(structural)

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

    /** Resolution order for a field's type. First match wins.
      *
      * `renderSelf` must stay first: it recognises the root type being derived and calls back into the instance under
      * construction. Were `renderSummoned` to run first it would find that same instance through implicit search and
      * inline it into itself.
      *
      * `renderSummoned` sits ahead of `renderPrimitive` so that a user-supplied `given Describe[T]` really does win
      * for every `T`, including `String`, `Char` and the numeric primitives. The reverse order made the built-in
      * scalar renderings unoverridable while the README promised the opposite; the documented contract is the one
      * worth keeping, and the cost is one implicit search per scalar field at expansion time only.
      */
    private def renderValue(rawType: TypeRepr, term: Term, nesting: Nesting): Expr[String] =
      val tpe = structural(rawType)
      renderSelf(tpe, term)
        .orElse(renderSummoned(tpe, term))
        .orElse(renderPrimitive(tpe, term))
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
      val declared   = structural(owner.memberType(field))
      val symbol     = namingSymbolOf(declared)
      val qualified  = qualifiedNameOf(symbol)
      val name       = Expr(field.name)
      val simple     = Expr(simpleNameOf(symbol))
      val full       = Expr(qualified)
      val compressed = Expr(Rendering.compressPackages(qualified))
      '{ Rendering.field($name, $simple, $full, $compressed, $value, $conf) }

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
      val name        = nameExpr(tpe.typeSymbol)
      val fields      = Expr.ofList(rendered)
      '{ Rendering.assemble($name, $fields, $conf) }

    private def renderProduct(tpe: TypeRepr, term: Term, nesting: Nesting): Expr[String] =
      tpe.asType match
        case '[t] =>
          val value = coerce[t](term)
          '{
            Rendering.nullOr[t](
              $value,
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
        case '[EmptyTuple]   => Nil
        // Not a tuple at all: the mirror was misread. Reported as "no children", which routes `appliedChildType`
        // through `unifyChild` and, failing that, to an explicit refusal — never to a silently wrong rendering.
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

    /** The three members below take their own `Quotes` rather than using the one [[Expansion]] captured, and that is
      * load-bearing rather than stylistic. They are the only members that receive a bare `Expr` created *inside* a
      * splice — the `bound` lambda parameter that [[renderSealed]] introduces — and re-embed it in a quote of their
      * own. Building that quote with the captured outer `Quotes` extrudes `bound` from the splice that created it,
      * which `-Xcheck-macros` rejects as a `ScopeException`; taking `(using Quotes)` means the splice's own context
      * wins resolution and the expression never leaves its scope. The sibling product path escapes the same check only
      * because it launders its `bound` through `.asTerm`, which drops the scope tag — so it is unchecked, not exempt.
      */
    private def branchCondition(child: Symbol, scrutinee: Expr[Any])(using Quotes): Expr[Boolean] =
      moduleOf(child) match
        case Some(module) =>
          val singleton = Ref(module).asExpr
          '{ $scrutinee == $singleton }
        case None         =>
          erasedChildType(child).asType match
            case '[c] => '{ $scrutinee.isInstanceOf[c] }

    private def renderChild(
        parent: TypeRepr,
        child: Symbol,
        scrutinee: Expr[Any],
        nesting: Nesting,
    )(using Quotes): Expr[String] =
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
    )(using Quotes): Expr[String] =
      children match
        case Nil           => '{ Rendering.opaque($scrutinee) }
        case child :: rest =>
          // `scrutinee` is bound by the enclosing splice, not by this quote, so all three pieces are built before it.
          val matches  = branchCondition(child, scrutinee)
          val rendered = renderChild(parent, child, scrutinee, nesting)
          val fallback = renderBranches(parent, rest, scrutinee, nesting)
          '{ if $matches then $rendered else $fallback }

    private def renderSealed(tpe: TypeRepr, term: Term, nesting: Nesting): Expr[String] =
      val children    = tpe.typeSymbol.children
      val nextNesting = nesting.dispatching(tpe)
      val value       = term.asExprOf[Any]
      '{
        Rendering.nullOr[Any](
          $value,
          (bound: Any) => ${ renderBranches(tpe, children, 'bound, nextNesting) },
        )
      }

    // ------------------------------------------------------------------- root

    def renderRoot(root: Expr[T]): Expr[String] =
      renderStructure(rootType, root.asTerm, Nesting(0, 0, Nil, branch = false))
        .getOrElse(
          report.errorAndAbort(
            s"${rootType.show} is not a case class, case object, sealed trait or enum, " +
              "so Describe cannot be derived for it."
          )
        )
