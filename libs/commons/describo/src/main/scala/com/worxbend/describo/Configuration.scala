package com.worxbend.describo

/** Rendering options for [[PrettyPrintable]].
  *
  * Kept field-for-field identical with com.worxbend.reveal.Configuration. Any change here must be mirrored there and in
  * both READMEs.
  *
  * This is a rendering-options DTO that is always consumed with named arguments, which is the one shape where boolean
  * members are self-documenting at the call site. It is also the single, narrow, reviewed exception to the repository's
  * `noDefaultArgs` guidance: a sixteen-field options record without defaults is unusable, and the same record has to be
  * duplicated verbatim in a second package that cannot share code with this one.
  *
  * @param useFieldNames
  *   render `field = value` instead of a bare `value`
  * @param useTypeNames
  *   render the declared type of every field
  * @param fullyQualifiedClassName
  *   render fully qualified names instead of simple names
  * @param shortPackagePrefix
  *   compress leading lowercase package segments to their first character
  * @param fieldsSeparator
  *   inter-field separator; used verbatim on a single line, trailing-stripped in multiline
  * @param fieldNamePrefix
  *   inserted before every field name
  * @param fieldNameSuffix
  *   inserted after every field name
  * @param fieldNameAndValueSeparator
  *   inserted between the field name (or its type) and the value
  * @param fieldNameAndTypeNameSeparator
  *   inserted between the field name and the type name
  * @param typeNamePrefix
  *   inserted before every type name
  * @param typeNameSuffix
  *   inserted after every type name
  * @param valuePrefix
  *   inserted before every rendered value
  * @param valueSuffix
  *   inserted after every rendered value
  * @param multiline
  *   always render one field per line
  * @param multilineIndent
  *   per-field indentation used in multiline layout
  * @param multilineIfFieldsAreGreaterOrEqual
  *   switch to multiline once this many fields are rendered; any value `<= 0` disables the threshold entirely
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

  /** The canonical instance: the default argument of `asString` and the baseline every README example assumes. */
  val default: Configuration = Configuration()
