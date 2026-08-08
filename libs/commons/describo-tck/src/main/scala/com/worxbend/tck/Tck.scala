package com.worxbend.tck

/** The parity specification, as data.
  *
  * Every entry is an obligation both renderers must satisfy byte for byte. Before this kit existed the contract was
  * three literal strings in each module's own suite, which is how the modules came to disagree on repeated `@Redacted`
  * while both suites stayed green: the divergence was in a shape neither fixture covered.
  *
  * Adding a case here fails every adapter that does not implement its fixture, which is the intended pressure — the
  * catalogue is the spec, and an adapter that cannot meet it is the thing that is wrong.
  */
object Tck:

  private val flat: TckConfiguration     = TckConfiguration.flat
  private val expanded: TckConfiguration = TckConfiguration.expanded

  /** Scalar values and the escaping rules. */
  private val scalars: List[TckCase] = List(
    TckCase(
      "primitives",
      flat,
      """Primitives(c = 'x', by = 1, sh = 2, i = 3, l = 4, f = 5.5, d = 6.5, bool = true)""",
      "every primitive renders bare; only Char is quoted, with single quotes",
    ),
    TckCase(
      "big-numbers",
      flat,
      """BigNumbers(dec = 1000.50, int = 99999999999999999999)""",
      "BigDecimal keeps its scale and BigInt renders bare",
    ),
    TckCase(
      "string-plain",
      flat,
      """StringHolder(s = "hello")""",
      "a string is double quoted",
    ),
    TckCase(
      "string-empty",
      flat,
      """StringHolder(s = "")""",
      "an empty string renders as empty quotes, not as nothing",
    ),
    TckCase(
      "string-null",
      flat,
      """StringHolder(s = null)""",
      "a null string renders as the bare word null, never quoted and never throwing",
    ),
    TckCase(
      "string-separators",
      flat,
      """StringHolder(s = "a,b)c(d")""",
      "commas and parentheses inside a string must not be confusable with structure",
    ),
    TckCase(
      "string-escapes",
      flat,
      """StringHolder(s = "q\"b\\s\nn\tt\rr")""",
      "quote, backslash, newline, tab and carriage return are escaped; backslash first",
    ),
    TckCase(
      "char-quote",
      flat,
      """CharHolder(c = '\'')""",
      "a single quote inside a Char is escaped",
    ),
  )

  /** Collections, maps, arrays and Option. */
  private val containers: List[TckCase] = List(
    TckCase("list-strings", flat, """Listish(xs = ["a", "b"])""", "elements follow the same value rules as fields"),
    TckCase("list-empty", flat, """Listish(xs = [])""", "an empty collection renders as empty brackets"),
    TckCase("list-null", flat, """Listish(xs = null)""", "a null collection is null, not empty brackets"),
    TckCase("set-ints", flat, """Setish(xs = [1])""", "a Set renders in the same brackets as a List"),
    TckCase("vector-ints", flat, """Vectorish(xs = [1, 2])""", "a Vector renders in the same brackets as a List"),
    TckCase("array-ints", flat, """Arrayish(xs = [1, 2])""", "an Array renders in brackets, never as its JVM identity"),
    TckCase("array-null", flat, """Arrayish(xs = null)""", "a null Array is null"),
    TckCase(
      "nested-collections",
      flat,
      """Nestedish(xs = [["a"], ["b"]])""",
      "nesting recurses through the element renderer",
    ),
    TckCase("map-strings", flat, """Mapish(m = ["k" -> "v"])""", "a Map renders as bracketed arrow entries"),
    TckCase("map-int-keys", flat, """MapIntish(m = [1 -> "v"])""", "both sides of an entry follow the value rules"),
    TckCase("map-empty", flat, """Mapish(m = [])""", "an empty Map renders as empty brackets"),
    TckCase("map-null", flat, """Mapish(m = null)""", "a null Map is null"),
    TckCase("option-some", flat, """Optionish(o = Some("v"))""", "Some quotes its payload"),
    TckCase("option-none", flat, """Optionish(o = None)""", "None renders as the bare word None"),
    TckCase("option-null", flat, """Optionish(o = null)""", "a null Option is null, distinct from None"),
    TckCase("java-list", flat, """JavaListish(xs = ["a"])""", "a java.util.List renders exactly like a Scala List"),
    TckCase("java-set", flat, """JavaSetish(xs = ["a"])""", "a java.util.Set renders exactly like a Scala Set"),
    TckCase(
      "java-map",
      flat,
      """JavaMapish(m = ["k" -> "v"])""",
      "a java.util.Map renders in brackets, matching Scala Map and never in braces",
    ),
  )

  /** Product shapes: nesting, value classes, case objects, sealed families. */
  private val structures: List[TckCase] = List(
    TckCase("empty-case-class", flat, """Empty()""", "an empty case class renders with empty parentheses"),
    TckCase("single-field", flat, """Single(only = 1)""", "a single field needs no separator"),
    TckCase("nested", flat, """Outer(inner = Inner(v = 1), tag = "t")""", "a nested product renders inline"),
    TckCase("nested-null", flat, """Outer(inner = null, tag = "t")""", "a null nested product is null"),
    TckCase("value-class", flat, """ValueHolder(id = 7)""", "a value class unwraps to its payload"),
    TckCase("case-object", flat, """CaseObjectHolder(o = TheObject)""", "a case object renders as its bare name"),
    TckCase("sealed-child", flat, """SealedHolder(s = Child(v = 1))""", "a sealed family dispatches to its child"),
    TckCase("enum-singleton", flat, """EnumHolder(e = Red)""", "a singleton enum case renders as its bare name"),
    TckCase("enum-fields", flat, """EnumFieldsHolder(e = Sized(n = 2))""", "an enum case with fields is structural"),
    TckCase("either-right", flat, """EitherHolder(e = Right(value = 1))""", "Either dispatches to Right"),
    TckCase("either-left", flat, """EitherHolder(e = Left(value = "e"))""", "Either dispatches to Left"),
    TckCase(
      "tuple-scalars",
      flat,
      """TupleHolder(pair = Tuple2(_1 = 1, _2 = "a"))""",
      "a tuple is expanded structurally, not handed to its own toString",
    ),
    TckCase(
      "tuple-nested",
      flat,
      """TupleNestedHolder(pair = Tuple2(_1 = Inner(v = 1), _2 = "t"))""",
      "a tuple's elements go back through the normal resolution, so an element's own instance still applies",
    ),
    TckCase(
      "deep-recursion",
      flat,
      "Node(v = 1, next = Some(Node(v = 2, next = Some(Node(v = 3, next = Some(Node(v = 4, next = None)))))))",
      "a self-recursive type renders to arbitrary runtime depth through its own instance; depth parity is cheap to " +
        "pin now that the fixtures are data, and neither engine caps it at render time",
    ),
  )

  /** Annotation semantics — the reason the libraries exist. */
  private val annotations: List[TckCase] = List(
    TckCase("redacted-default", flat, """Secretish(secret = <redacted>, tag = "t")""", "the default replacement"),
    TckCase(
      "redacted-custom",
      flat,
      """SecretCustom(secret = ***, tag = "t")""",
      "a custom replacement is used verbatim",
    ),
    TckCase(
      "redacted-null",
      flat,
      """Secretish(secret = <redacted>, tag = "t")""",
      "a redacted field is never dereferenced, so a null secret still renders its replacement",
    ),
    TckCase("excluded", flat, """Excludish(tag = "t")""", "@Excluded omits the field entirely"),
    TckCase("transient-field", flat, """Transientish(tag = "t")""", "@transient behaves exactly like @Excluded"),
    TckCase(
      "redacted-and-excluded",
      flat,
      """BothAnnotations(tag = "t")""",
      "exclusion beats redaction; the field is omitted, not redacted",
    ),
    TckCase(
      "excluded-and-redacted",
      flat,
      """BothAnnotationsReversed(tag = "t")""",
      "source order of the two annotations is irrelevant",
    ),
    TckCase(
      "repeated-redacted",
      flat,
      """RepeatedRedaction(secret = first, tag = "t")""",
      "the @Redacted written first in source order wins; this is the case the two engines silently disagreed on",
    ),
    TckCase(
      "nested-redaction",
      flat,
      """RedactedOuter(inner = RedactedInner(secret = <redacted>), tag = "t")""",
      "redaction composes through nesting, which is the property a hand-written toString loses",
    ),
    TckCase(
      "collection-redaction",
      flat,
      """RedactedCollection(xs = [RedactedInner(secret = <redacted>)])""",
      "redaction composes through collection elements",
    ),
    TckCase(
      "option-redaction",
      flat,
      """RedactedOption(o = Some(RedactedInner(secret = <redacted>)))""",
      "redaction composes through an Option payload",
    ),
    TckCase(
      "map-value-redaction",
      flat,
      """RedactedMap(m = ["k" -> RedactedInner(secret = <redacted>)])""",
      "redaction composes through Map values",
    ),
    TckCase("all-excluded", flat, """AllExcluded()""", "a product whose fields are all omitted keeps its parentheses"),
  )

  /** Layout and the configuration knobs. */
  private val layout: List[TckCase] = List(
    TckCase(
      "layout-threshold",
      TckConfiguration(multilineIfFieldsAreGreaterOrEqual = 2),
      "Pair(\n  a = 1,\n  b = 2\n)",
      "the multiline layout triggers exactly at the threshold",
    ),
    TckCase(
      "layout-threshold",
      TckConfiguration(multilineIfFieldsAreGreaterOrEqual = 3),
      """Pair(a = 1, b = 2)""",
      "one field below the threshold stays on a single line",
    ),
    TckCase(
      "layout-threshold",
      TckConfiguration(multilineIfFieldsAreGreaterOrEqual = 0),
      """Pair(a = 1, b = 2)""",
      "a threshold of zero disables the rule rather than forcing multiline",
    ),
    TckCase(
      "layout-threshold",
      expanded,
      "Pair(\n  a = 1,\n  b = 2\n)",
      "multiline = true forces the layout regardless of field count",
    ),
    TckCase(
      "layout-threshold",
      TckConfiguration(multiline = true, multilineIndent = "    "),
      "Pair(\n    a = 1,\n    b = 2\n)",
      "multilineIndent controls the per-field indent",
    ),
    TckCase(
      "empty-case-class",
      expanded,
      """Empty()""",
      "a product with no rendered fields stays on one line even when multiline is requested",
    ),
    TckCase(
      "layout-threshold",
      TckConfiguration(useFieldNames = false, multilineIfFieldsAreGreaterOrEqual = -1),
      """Pair(1, 2)""",
      "useFieldNames = false drops names entirely",
    ),
    TckCase(
      "layout-threshold",
      TckConfiguration(fieldsSeparator = " | ", multilineIfFieldsAreGreaterOrEqual = -1),
      """Pair(a = 1 | b = 2)""",
      "fieldsSeparator is used verbatim on a single line",
    ),
    TckCase(
      "layout-threshold",
      TckConfiguration(
        fieldNamePrefix = "<",
        fieldNameSuffix = ">",
        fieldNameAndValueSeparator = " -> ",
        valuePrefix = "[",
        valueSuffix = "]",
        multilineIfFieldsAreGreaterOrEqual = -1,
      ),
      """Pair(<a> -> [1], <b> -> [2])""",
      "name affixes, the name/value separator and value affixes all apply",
    ),
    TckCase(
      "layout-threshold",
      TckConfiguration(useTypeNames = true, multilineIfFieldsAreGreaterOrEqual = -1),
      """Pair(a: Int = 1, b: Int = 2)""",
      "useTypeNames spells the declared type, never the runtime class",
    ),
    TckCase(
      "list-strings",
      TckConfiguration(useTypeNames = true, multilineIfFieldsAreGreaterOrEqual = -1),
      """Listish(xs: List = ["a", "b"])""",
      "a List field reports List, never $colon$colon",
    ),
    TckCase(
      "map-strings",
      TckConfiguration(useTypeNames = true, multilineIfFieldsAreGreaterOrEqual = -1),
      """Mapish(m: Map = ["k" -> "v"])""",
      "a Map field reports Map, never Map1 or Map2",
    ),
    TckCase(
      "option-some",
      TckConfiguration(useTypeNames = true, multilineIfFieldsAreGreaterOrEqual = -1),
      """Optionish(o: Option = Some("v"))""",
      "an Option field reports Option, never Some",
    ),
    TckCase(
      "nested",
      TckConfiguration(multiline = true),
      "Outer(\n  inner = Inner(\n  v = 1\n),\n  tag = \"t\"\n)",
      "a nested multiline render is inserted verbatim, so its closing paren sits at the outer indent — ugly, " +
        "documented, and pinned here so it cannot change silently in only one of the two engines",
    ),
  )

  /** The naming options: qualified names, package compression, and the type-name affixes.
    *
    * These were the last public options the catalogue did not exercise, which meant the two engines could have
    * disagreed on any of them while every test stayed green.
    */
  private val naming: List[TckCase] = List(
    TckCase(
      "qualified-names",
      TckConfiguration(
        fullyQualifiedClassName = true,
        shortPackagePrefix = false,
        multilineIfFieldsAreGreaterOrEqual = -1,
      ),
      """{pkg}.TckQualified(a = 1, s = "v")""",
      "fullyQualifiedClassName spells the product's whole name; {pkg} is each adapter's own fixture package",
    ),
    TckCase(
      "qualified-names",
      TckConfiguration(
        fullyQualifiedClassName = true,
        shortPackagePrefix = true,
        multilineIfFieldsAreGreaterOrEqual = -1,
      ),
      """c.w.d.TckQualified(a = 1, s = "v")""",
      "shortPackagePrefix compresses leading lowercase segments; both modules genuinely compress to c.w.d",
    ),
    TckCase(
      "qualified-names",
      TckConfiguration(
        fullyQualifiedClassName = true,
        shortPackagePrefix = false,
        useTypeNames = true,
        multilineIfFieldsAreGreaterOrEqual = -1,
      ),
      """{pkg}.TckQualified(a: scala.Int = 1, s: java.lang.String = "v")""",
      "fullyQualifiedClassName qualifies field type names too, and those are not module-specific",
    ),
    TckCase(
      "qualified-names",
      TckConfiguration(
        fullyQualifiedClassName = true,
        shortPackagePrefix = true,
        useTypeNames = true,
        multilineIfFieldsAreGreaterOrEqual = -1,
      ),
      """c.w.d.TckQualified(a: s.Int = 1, s: j.l.String = "v")""",
      "package compression applies to field type names as well as to the product's own name",
    ),
    TckCase(
      "layout-threshold",
      TckConfiguration(
        useTypeNames = true,
        fieldNameAndTypeNameSeparator = "::",
        typeNamePrefix = "<",
        typeNameSuffix = ">",
        multilineIfFieldsAreGreaterOrEqual = -1,
      ),
      """Pair(a::<Int> = 1, b::<Int> = 2)""",
      "fieldNameAndTypeNameSeparator, typeNamePrefix and typeNameSuffix all apply to the type name group",
    ),
    TckCase(
      "layout-threshold",
      TckConfiguration(useFieldNames = false, useTypeNames = true, multilineIfFieldsAreGreaterOrEqual = -1),
      """Pair(1, 2)""",
      "useFieldNames = false drops the type name too, because the type belongs to the name group",
    ),
  )

  /** Every obligation, in a stable order. */
  val cases: List[TckCase] = scalars ++ containers ++ structures ++ annotations ++ layout ++ naming

  /** Distinct fixture ids an adapter must be able to build. */
  val fixtureIds: List[String] = cases.map(_.fixtureId).distinct.sorted
