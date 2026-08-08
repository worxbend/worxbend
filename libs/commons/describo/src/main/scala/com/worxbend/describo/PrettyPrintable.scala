package com.worxbend.describo

import scala.deriving.Mirror
import scala.jdk.CollectionConverters.*

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Period
import java.time.ZonedDateTime

import magnolia1.*

/** Renders a value as a configurable string. Only read access to fields is required.
  *
  * The instance carries the declared type of the value it renders ([[printedType]]). Field types are therefore printed
  * without ever dereferencing the field, which is what makes redacted, excluded and `null` fields safe.
  */
trait PrettyPrintable[T]:

  /** The declared type of `T`, as it should be printed. */
  def printedType: PrintedType

  extension (x: T)

    def asString(
        using configuration: Configuration = Configuration.default
    ): String

trait GenericPrint extends AutoDerivation[PrettyPrintable]:

  override def join[T](ctx: CaseClass[Typeclass, T]): PrettyPrintable[T] =
    val tpe: PrintedType = PrintedType(ctx.typeInfo.short, ctx.typeInfo.full.stripSuffix("$"))

    // Annotation scanning is configuration-independent, so it happens once per typeclass instance rather than once per
    // call, and it is materialised as a Vector so that neither the filter nor the size is recomputed on every render.
    val plan: Vector[(CaseClass.Param[Typeclass, T], FieldRule)] =
      ctx.params.iterator.map(param => (param, FieldRule.of(param.annotations))).toVector

    new PrettyPrintable[T]:

      override val printedType: PrintedType = tpe

      extension (value: T)

        override def asString(
            using conf: Configuration
        ): String =
          if ctx.isObject || isSingleton(value) then tpe.render
          else layout(tpe, renderedFields(value))

      /** A parameterless Scala 3 enum case is a singleton value rather than a module, so Magnolia reports
        * `isObject = false` for it and `join` has no compile-time way to tell it apart from an empty case class. Every
        * such value implements `Mirror.Singleton`, so one `instanceof` on the value already in hand settles it. No
        * field is dereferenced and no reflection is involved.
        */
      private def isSingleton(value: T): Boolean = plan.isEmpty && value.isInstanceOf[Mirror.Singleton]

      private def renderedFields(
          value: T
      )(using Configuration): Vector[String] =
        plan.flatMap: (param, rule) =>
          rule match
            case FieldRule.Omit                => Vector.empty
            case FieldRule.Redact(replacement) => Vector(field(param, replacement))
            case FieldRule.Render              =>
              Vector(field(param, Rendering.value(param.deref(value))(using param.typeclass)))

  override def split[T](ctx: SealedTrait[Typeclass, T]): PrettyPrintable[T] =
    val tpe: PrintedType = PrintedType(ctx.typeInfo.short, ctx.typeInfo.full.stripSuffix("$"))

    new PrettyPrintable[T]:

      override val printedType: PrintedType = tpe

      extension (value: T)

        override def asString(
            using conf: Configuration
        ): String = ctx.choose(value)(sub => Rendering.value(sub.value)(using sub.typeclass))

  private def field[T](
      param: CaseClass.Param[Typeclass, T],
      renderedValue: String,
  )(using conf: Configuration): String =
    val namePart =
      if !conf.useFieldNames then ""
      else
        val typePart =
          if conf.useTypeNames then
            conf.fieldNameAndTypeNameSeparator +
              conf.typeNamePrefix +
              param.typeclass.printedType.render +
              conf.typeNameSuffix
          else ""
        conf.fieldNamePrefix + param.label + conf.fieldNameSuffix + typePart + conf.fieldNameAndValueSeparator
    namePart + conf.valuePrefix + renderedValue + conf.valueSuffix

  private def layout(
      tpe: PrintedType,
      renderedFields: Vector[String],
  )(using conf: Configuration): String =
    Layout.of(renderedFields.size) match
      case Layout.SingleLine =>
        renderedFields.mkString(s"${tpe.render}(", conf.fieldsSeparator, ")")
      case Layout.Multiline  =>
        // Only trailing whitespace is dropped, and only here, because it would otherwise be invisible whitespace at the
        // end of every line. Leading whitespace is preserved.
        renderedFields
          .map(conf.multilineIndent + _)
          .mkString(s"${tpe.render}(\n", conf.fieldsSeparator.stripTrailing() + "\n", "\n)")

/** The built-in instances, and the four factories for writing your own.
  *
  * The typeclass is invariant, so only the exact declared type of a field resolves. A field of a collection type
  * without a built-in instance needs one of its own; [[PrettyPrintable.instance]], [[PrettyPrintable.collection]],
  * [[PrettyPrintable.mapping]] and [[PrettyPrintable.valueClass]] exist so that writing one is a one-liner that inherits this
  * module's escaping, `null` handling and element separator instead of re-implementing them.
  */
object PrettyPrintable extends GenericPrint:

  /** Instance for a type rendered by a plain function of the value, independently of the configuration.
    *
    * {{{
    * given PrettyPrintable[java.util.UUID] = PrettyPrintable.instance("UUID", "java.util.UUID")(_.toString)
    * }}}
    *
    * The rendered string is used verbatim, so a type that should appear quoted has to quote itself.
    *
    * @param simpleName
    *   the declared type's own name, printed under `useTypeNames`
    * @param qualifiedName
    *   its fully qualified name, printed under `fullyQualifiedClassName`
    */
  def instance[T](
      simpleName: String,
      qualifiedName: String,
  )(render: T => String): PrettyPrintable[T] =
    configured[T](PrintedType(simpleName, qualifiedName))((x, _) => render(x))

  /** Instance for a container rendered as `[a, b, c]`, with the elements rendered by their own instance.
    *
    * {{{
    * given PrettyPrintable[java.util.ArrayDeque[String]] =
    *   PrettyPrintable.collection("ArrayDeque", "java.util.ArrayDeque")(_.iterator.asScala)
    * }}}
    *
    * @param elements
    *   reads the elements in the order they should be printed
    */
  def collection[C, T](
      simpleName: String,
      qualifiedName: String,
  )(elements: C => Iterator[T])(using printableT: PrettyPrintable[T]): PrettyPrintable[C] =
    configured[C](PrintedType(simpleName, qualifiedName)): (x, conf) =>
      Rendering.elements(elements(x))(using printableT)(using conf)

  /** Instance for a container rendered as `[k -> v]`, with keys and values rendered by their own instances.
    *
    * {{{
    * given PrettyPrintable[java.util.TreeMap[String, Int]] =
    *   PrettyPrintable.mapping("TreeMap", "java.util.TreeMap")(_.asScala.iterator)
    * }}}
    *
    * @param entries
    *   reads the entries in the order they should be printed
    */
  def mapping[C, K, V](
      simpleName: String,
      qualifiedName: String,
  )(entries: C => Iterator[(K, V)])(using keyP: PrettyPrintable[K], valueP: PrettyPrintable[V]): PrettyPrintable[C] =
    configured[C](PrintedType(simpleName, qualifiedName)): (x, conf) =>
      Rendering.entries(entries(x))(using keyP, valueP)(using conf)

  /** Instance for a value class, rendered as its payload but named after the wrapper.
    *
    * Scala 3 synthesises no `Mirror` for a value class, so `derives PrettyPrintable` cannot be used on one — Magnolia never
    * sees it. This is the supported way to give a value class an instance:
    *
    * {{{
    * final case class UserId(value: String) extends AnyVal
    * object UserId:
    *   given PrettyPrintable[UserId] = PrettyPrintable.valueClass("UserId", "com.example.UserId")(_.value)
    * }}}
    *
    * @param simpleName
    *   the wrapper's own name, not the payload's
    * @param qualifiedName
    *   the wrapper's fully qualified name
    * @param unwrap
    *   reads the single payload
    */
  def valueClass[W, U](
      simpleName: String,
      qualifiedName: String,
  )(unwrap: W => U)(using printableU: PrettyPrintable[U]): PrettyPrintable[W] =
    configured[W](PrintedType(simpleName, qualifiedName)): (x, conf) =>
      Rendering.value(unwrap(x))(using printableU)(using conf)

  private def configured[T](
      tpe: PrintedType
  )(render: (T, Configuration) => String): PrettyPrintable[T] =
    new PrettyPrintable[T]:

      override val printedType: PrintedType = tpe

      extension (x: T)

        override def asString(
            using configuration: Configuration
        ): String = render(x, configuration)

  private def quotedChar(x: Char): String =
    s"'${Rendering.escaped(x.toString).replace("'", "\\'")}'"

  given string: PrettyPrintable[String] =
    instance[String]("String", "java.lang.String")(x => s"\"${Rendering.escaped(x)}\"")

  given char: PrettyPrintable[Char] = instance[Char]("Char", "scala.Char")(quotedChar)

  given int: PrettyPrintable[Int]     = instance[Int]("Int", "scala.Int")(_.toString)
  given long: PrettyPrintable[Long]   = instance[Long]("Long", "scala.Long")(_.toString)
  given short: PrettyPrintable[Short] = instance[Short]("Short", "scala.Short")(_.toString)
  given byte: PrettyPrintable[Byte]   = instance[Byte]("Byte", "scala.Byte")(_.toString)

  given double: PrettyPrintable[Double]   = instance[Double]("Double", "scala.Double")(_.toString)
  given float: PrettyPrintable[Float]     = instance[Float]("Float", "scala.Float")(_.toString)
  given boolean: PrettyPrintable[Boolean] = instance[Boolean]("Boolean", "scala.Boolean")(_.toString)

  given bigInt: PrettyPrintable[BigInt]         = instance[BigInt]("BigInt", "scala.math.BigInt")(_.toString)

  given bigDecimal: PrettyPrintable[BigDecimal] =
    instance[BigDecimal]("BigDecimal", "scala.math.BigDecimal")(_.toString)

  given javaInteger: PrettyPrintable[java.lang.Integer] =
    instance[java.lang.Integer]("Integer", "java.lang.Integer")(_.toString)

  given javaCharacter: PrettyPrintable[java.lang.Character] =
    instance[java.lang.Character]("Character", "java.lang.Character")(x => quotedChar(x.charValue))

  given localDate: PrettyPrintable[LocalDate] = instance[LocalDate]("LocalDate", "java.time.LocalDate")(_.toString)
  given localTime: PrettyPrintable[LocalTime] = instance[LocalTime]("LocalTime", "java.time.LocalTime")(_.toString)
  given instant: PrettyPrintable[Instant]     = instance[Instant]("Instant", "java.time.Instant")(_.toString)
  given duration: PrettyPrintable[Duration]   = instance[Duration]("Duration", "java.time.Duration")(_.toString)
  given period: PrettyPrintable[Period]       = instance[Period]("Period", "java.time.Period")(_.toString)

  given zonedDateTime: PrettyPrintable[ZonedDateTime] =
    instance[ZonedDateTime]("ZonedDateTime", "java.time.ZonedDateTime")(_.toString)

  given offsetDateTime: PrettyPrintable[OffsetDateTime] =
    instance[OffsetDateTime]("OffsetDateTime", "java.time.OffsetDateTime")(_.toString)

  given offsetTime: PrettyPrintable[OffsetTime] = instance[OffsetTime]("OffsetTime", "java.time.OffsetTime")(_.toString)

  // The typeclass stays invariant on purpose. Contravariance would let a single Iterable instance serve List, Set and
  // Vector, but it collides with Magnolia's AutoDerivation for types that also have a Mirror. Invariance is what
  // guarantees that a `List[String]` field picks `list` and not `iterable`, so the bodies are shared through the
  // factories above instead of through the variance annotation.

  given iterable[T](using PrettyPrintable[T]): PrettyPrintable[Iterable[T]] =
    collection[Iterable[T], T]("Iterable", "scala.collection.Iterable")(_.iterator)

  given seq[T](using PrettyPrintable[T]): PrettyPrintable[Seq[T]] =
    collection[Seq[T], T]("Seq", "scala.collection.immutable.Seq")(_.iterator)

  given indexedSeq[T](using PrettyPrintable[T]): PrettyPrintable[IndexedSeq[T]] =
    collection[IndexedSeq[T], T]("IndexedSeq", "scala.collection.immutable.IndexedSeq")(_.iterator)

  given list[T](using PrettyPrintable[T]): PrettyPrintable[List[T]] =
    collection[List[T], T]("List", "scala.collection.immutable.List")(_.iterator)

  given vector[T](using PrettyPrintable[T]): PrettyPrintable[Vector[T]] =
    collection[Vector[T], T]("Vector", "scala.collection.immutable.Vector")(_.iterator)

  given set[T](using PrettyPrintable[T]): PrettyPrintable[Set[T]] =
    collection[Set[T], T]("Set", "scala.collection.immutable.Set")(_.iterator)

  given array[T](using PrettyPrintable[T]): PrettyPrintable[Array[T]] =
    collection[Array[T], T]("Array", "scala.Array")(_.iterator)

  given javaList[T](using PrettyPrintable[T]): PrettyPrintable[java.util.List[T]] =
    collection[java.util.List[T], T]("List", "java.util.List")(_.asScala.iterator)

  given javaArrayList[T](using PrettyPrintable[T]): PrettyPrintable[java.util.ArrayList[T]] =
    collection[java.util.ArrayList[T], T]("ArrayList", "java.util.ArrayList")(_.asScala.iterator)

  given javaLinkedList[T](using PrettyPrintable[T]): PrettyPrintable[java.util.LinkedList[T]] =
    collection[java.util.LinkedList[T], T]("LinkedList", "java.util.LinkedList")(_.asScala.iterator)

  given javaSet[T](using PrettyPrintable[T]): PrettyPrintable[java.util.Set[T]] =
    collection[java.util.Set[T], T]("Set", "java.util.Set")(_.asScala.iterator)

  given javaHashSet[T](using PrettyPrintable[T]): PrettyPrintable[java.util.HashSet[T]] =
    collection[java.util.HashSet[T], T]("HashSet", "java.util.HashSet")(_.asScala.iterator)

  given map[K, V](using PrettyPrintable[K], PrettyPrintable[V]): PrettyPrintable[Map[K, V]] =
    mapping[Map[K, V], K, V]("Map", "scala.collection.immutable.Map")(_.iterator)

  given javaMap[K, V](using PrettyPrintable[K], PrettyPrintable[V]): PrettyPrintable[java.util.Map[K, V]] =
    mapping[java.util.Map[K, V], K, V]("Map", "java.util.Map")(_.asScala.iterator)

  given javaHashMap[K, V](using PrettyPrintable[K], PrettyPrintable[V]): PrettyPrintable[java.util.HashMap[K, V]] =
    mapping[java.util.HashMap[K, V], K, V]("HashMap", "java.util.HashMap")(_.asScala.iterator)

  given option[T](using printableT: PrettyPrintable[T]): PrettyPrintable[Option[T]] =
    configured[Option[T]](PrintedType("Option", "scala.Option")): (x, conf) =>
      x match
        case Some(payload) => s"Some(${Rendering.value(payload)(using printableT)(using conf)})"
        case None          => "None"
