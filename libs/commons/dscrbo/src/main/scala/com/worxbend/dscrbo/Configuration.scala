package com.worxbend.dscrbo

/** Rendering options consumed by [[Describe]].
  *
  * Kept field-for-field identical with com.worxbend.describo.Configuration. Any change here must be mirrored there and
  * in both READMEs.
  *
  * This is a rendering-options DTO, always constructed with named arguments, which is the one shape where a flat record
  * of booleans stays self-documenting at the call site. It deliberately keeps default arguments — the single, narrow,
  * reviewed exception to the repository's `noDefaultArgs` guidance — because a sixteen-field options record without
  * defaults is unusable.
  *
  * @param useFieldNames
  *   render `name = value` instead of a bare `value`.
  * @param useTypeNames
  *   render the field's declared type between the field name and the value.
  * @param fullyQualifiedClassName
  *   render fully qualified names instead of simple names.
  * @param shortPackagePrefix
  *   when qualified names are used, compress leading lowercase package segments to their first character.
  * @param fieldsSeparator
  *   inter-field separator, used verbatim on a single line and with trailing whitespace stripped when multiline.
  * @param multilineIfFieldsAreGreaterOrEqual
  *   render multiline once this many fields survive exclusion; any value `<= 0` disables the threshold.
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
    * differently here than in `com.worxbend.describo`, and would silently satisfy a downstream `using Configuration`
    * the caller forgot to provide.
    */
  val default: Configuration = Configuration()
