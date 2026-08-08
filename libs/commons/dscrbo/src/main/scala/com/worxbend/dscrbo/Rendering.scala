package com.worxbend.dscrbo

/** Runtime support for macro-generated code.
  *
  * This object is public only because the code emitted by [[Describe.derived]] and [[ToString.derived]] refers to it
  * from the user's compilation unit. It is not part of the intended user-facing API and carries no compatibility
  * promise beyond what the macro needs. Members the emitted code never names are narrowed accordingly: helpers used
  * only from inside this object are `private`, and helpers evaluated at macro-expansion time are `private[dscrbo]`.
  *
  * Everything here is total: no member throws, and every member renders a `null` argument as the four characters
  * `null`.
  */
object Rendering:

  /** How a rendered type lays its fields out. Decided exactly once per render. */
  private enum Layout:

    case SingleLine
    case Multiline

  /** The rendering of a `null` value, in every position. */
  private val NullLiteral: String = "null"

  /** The separator between the elements of a collection, and between the entries of a map. Never configurable: the
    * `fieldsSeparator` knob is an inter-field one.
    */
  private val ElementSeparator: String = ", "

  /** Escapes a string body: backslash, double quote, newline, carriage return and tab, in that order. */
  private def escape(value: String): String =
    value
      .replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\n", "\\n")
      .replace("\r", "\\r")
      .replace("\t", "\\t")

  /** Wraps already rendered elements in the canonical brackets. */
  private def elements(rendered: IterableOnce[String]): String =
    rendered.iterator.mkString("[", ElementSeparator, "]")

  /** Renders one map entry from its already rendered key and value. */
  private def entry(key: String, value: String): String = key + " -> " + value

  /** Adapts a Java iterator without reaching for `scala.jdk.CollectionConverters`. */
  private def scalaIterator[A](source: java.util.Iterator[A]): Iterator[A] =
    new Iterator[A]:
      override def hasNext: Boolean = source.hasNext
      override def next(): A        = source.next()

  private def layoutOf(renderedFieldCount: Int, conf: Configuration): Layout =
    val wantsMultiline =
      conf.multiline ||
      (conf.multilineIfFieldsAreGreaterOrEqual > 0 &&
      renderedFieldCount >= conf.multilineIfFieldsAreGreaterOrEqual)
    if renderedFieldCount == 0 || !wantsMultiline then Layout.SingleLine else Layout.Multiline

  /** Renders a string as a double-quoted, escaped literal. `null` renders as `null`, unquoted. */
  def string(value: String): String =
    if value == null then NullLiteral else "\"" + escape(value) + "\""

  /** Renders a character as a single-quoted, escaped literal. */
  def char(value: Char): String =
    val body =
      value match
        case '\\'  => "\\\\"
        case '\''  => "\\'"
        case '"'   => "\\\""
        case '\n'  => "\\n"
        case '\r'  => "\\r"
        case '\t'  => "\\t"
        case other => other.toString
    "'" + body + "'"

  /** Renders a value the macro cannot see into, via `toString`, null-safely. */
  def opaque(value: Any): String =
    if value == null then NullLiteral else value.toString

  /** Applies `render` unless `value` is `null`, in which case the value renders as `null`. */
  def nullOr[A](value: A, render: A => String): String =
    if value == null then NullLiteral else render(value)

  /** Delegates to an existing instance, null-safely. */
  def nested[A](value: A, instance: Describe[A], conf: Configuration): String =
    if value == null then NullLiteral else instance.describe(value)(using conf)

  /** Renders an `Option`; a `null` `Option` reference renders as `null`, not as `None`. */
  def optionValue[A](source: Option[A], renderElement: A => String): String =
    if source == null then NullLiteral else source.fold("None")(item => "Some(" + renderElement(item) + ")")

  /** Renders any Scala collection; a `null` reference renders as `null`, not as `[]`. */
  def iterableValue[A](source: Iterable[A], renderElement: A => String): String =
    if source == null then NullLiteral else elements(source.iterator.map(renderElement))

  /** Renders an `Array`; a `null` reference renders as `null`, not as `[]`. */
  def arrayValue[A](source: Array[A], renderElement: A => String): String =
    if source == null then NullLiteral else elements(source.iterator.map(renderElement))

  /** Renders a Scala `Map`; a `null` reference renders as `null`, not as `[]`. */
  def mapValue[K, V](
      source: scala.collection.Map[K, V],
      renderKey: K => String,
      renderValue: V => String,
  ): String =
    if source == null then NullLiteral
    else elements(source.iterator.map((key, value) => entry(renderKey(key), renderValue(value))))

  /** Renders any `java.lang.Iterable`, which covers the `java.util` list and set families. */
  def javaIterableValue[A](source: java.lang.Iterable[A], renderElement: A => String): String =
    if source == null then NullLiteral else elements(scalaIterator(source.iterator).map(renderElement))

  /** Renders a `java.util.Map`, in the same bracket form as a Scala `Map`. */
  def javaMapValue[K, V](
      source: java.util.Map[K, V],
      renderKey: K => String,
      renderValue: V => String,
  ): String =
    if source == null then NullLiteral
    else
      elements(
        scalaIterator(source.entrySet.iterator).map(item => entry(renderKey(item.getKey), renderValue(item.getValue)))
      )

  /** Compresses every leading segment whose first character is lowercase to that single character.
    *
    * `com.worxbend.dscrbo.Fixture` becomes `c.w.d.Fixture`; a name without packages is returned unchanged, with no
    * leading dot. Evaluated at macro-expansion time, so the generated code only ever sees the result.
    */
  private[dscrbo] def compressPackages(qualifiedName: String): String =
    val (packageSegments, rest) =
      qualifiedName.split('.').toList.span(segment => segment.nonEmpty && segment.head.isLower)
    (packageSegments.map(segment => segment.head.toString) ++ rest).mkString(".")

  /** Selects the spelling of a type name for the current configuration. All three are compile-time literals. */
  def selectName(
      simpleName: String,
      qualifiedName: String,
      compressedName: String,
      conf: Configuration,
  ): String =
    if conf.fullyQualifiedClassName then if conf.shortPackagePrefix then compressedName else qualifiedName
    else simpleName

  /** Renders one field from its name, the three spellings of its declared type, and its already rendered value. */
  def field(
      name: String,
      simpleTypeName: String,
      qualifiedTypeName: String,
      compressedTypeName: String,
      value: String,
      conf: Configuration,
  ): String =
    val typePart =
      if conf.useTypeNames then
        conf.fieldNameAndTypeNameSeparator +
          conf.typeNamePrefix +
          selectName(simpleTypeName, qualifiedTypeName, compressedTypeName, conf) +
          conf.typeNameSuffix
      else ""
    val namePart =
      if conf.useFieldNames then
        conf.fieldNamePrefix + name + conf.fieldNameSuffix + typePart + conf.fieldNameAndValueSeparator
      else ""
    namePart + conf.valuePrefix + value + conf.valueSuffix

  /** Assembles a rendered type from its name and its already rendered, already filtered fields. */
  def assemble(typeName: String, fields: List[String], conf: Configuration): String =
    layoutOf(fields.size, conf) match
      case Layout.SingleLine => typeName + "(" + fields.mkString(conf.fieldsSeparator) + ")"
      case Layout.Multiline  =>
        val separator = conf.fieldsSeparator.stripTrailing() + "\n"
        typeName + "(\n" + fields.map(conf.multilineIndent + _).mkString(separator) + "\n)"
