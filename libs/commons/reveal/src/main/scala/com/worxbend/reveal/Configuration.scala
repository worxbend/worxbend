package com.worxbend.reveal

/** Rendering options consumed by [[PrettyPrintable]].
  *
  * Kept field-for-field identical with com.worxbend.prettyprinto.Configuration. Any change here must be mirrored there and
  * in both READMEs.
  *
  * This is a rendering-options DTO, always constructed with named arguments, which is the one shape where a flat record
  * of booleans stays self-documenting at the call site. It deliberately keeps default arguments — the single, narrow,
  * reviewed exception to the repository's `noDefaultArgs` guidance — because a sixteen-field options record without
  * defaults is unusable.
  *
  * A field renders as
  * {{{
  * fieldNamePrefix name fieldNameSuffix
  *   [ fieldNameAndTypeNameSeparator typeNamePrefix Type typeNameSuffix ]
  *   fieldNameAndValueSeparator valuePrefix value valueSuffix
  * }}}
  * where the bracketed group appears only under `useTypeNames`, and the whole name group is dropped when
  * `useFieldNames` is off.
  *
  * @param useFieldNames
  *   render `field = value` instead of a bare `value`; when off, the type name goes too, as it belongs to the name
  *   group
  * @param useTypeNames
  *   render the *declared* type of every field — never the runtime class, so a `List` field reports `List` and not
  *   `$colon$colon`
  * @param fullyQualifiedClassName
  *   render fully qualified names instead of simple names, for the product's own name and for field type names alike
  * @param shortPackagePrefix
  *   compress leading lowercase package segments to their first character, turning `com.worxbend.example.Order` into
  *   `c.w.e.Order`; ignored unless `fullyQualifiedClassName` is set
  * @param fieldsSeparator
  *   inter-field separator; used verbatim on a single line, trailing-stripped in multiline so `", "` does not leave a
  *   trailing space at the end of every line
  * @param fieldNamePrefix
  *   inserted before every field name
  * @param fieldNameSuffix
  *   inserted after every field name, ahead of any type name
  * @param fieldNameAndValueSeparator
  *   inserted between the field name (or its type) and the value
  * @param fieldNameAndTypeNameSeparator
  *   inserted between the field name and the type name; only used under `useTypeNames`
  * @param typeNamePrefix
  *   inserted before every type name
  * @param typeNameSuffix
  *   inserted after every type name
  * @param valuePrefix
  *   inserted before every rendered value, `null` and redaction replacements included
  * @param valueSuffix
  *   inserted after every rendered value, `null` and redaction replacements included
  * @param multiline
  *   always render one field per line; a product with no rendered fields stays on one line regardless, having nothing
  *   to break
  * @param multilineIndent
  *   per-field indentation used in multiline layout; a nested value is inserted verbatim and is *not* re-indented
  * @param multilineIfFieldsAreGreaterOrEqual
  *   switch to multiline once this many fields are rendered — excluded and transient fields do not count — and any
  *   value `<= 0` disables the threshold entirely, leaving `multiline` as the only trigger
  */
final case class Configuration(
    useFieldNames:                      Boolean = true,
    useTypeNames:                       Boolean = false,
    fullyQualifiedClassName:            Boolean = false,
    shortPackagePrefix:                 Boolean = true,
    fieldsSeparator:                    String = ", ",
    fieldNamePrefix:                    String = "",
    fieldNameSuffix:                    String = "",
    fieldNameAndValueSeparator:         String = " = ",
    fieldNameAndTypeNameSeparator:      String = ": ",
    typeNamePrefix:                     String = "",
    typeNameSuffix:                     String = "",
    valuePrefix:                        String = "",
    valueSuffix:                        String = "",
    multiline:                          Boolean = false,
    multilineIndent:                    String = "  ",
    multilineIfFieldsAreGreaterOrEqual: Int = 5,
)

object Configuration:

  /** The canonical instance, and the default argument of `asString` and `ToString.derived`.
    *
    * Deliberately not also exposed as a `given`: an instance in `Configuration`'s implicit scope would always win
    * implicit search, which would make those default arguments unreachable, would make `AutoToString` behave
    * differently here than in `com.worxbend.prettyprinto`, and would silently satisfy a downstream `using Configuration`
    * the caller forgot to provide.
    */
  val default: Configuration = Configuration()
