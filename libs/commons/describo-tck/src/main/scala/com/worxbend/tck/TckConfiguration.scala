package com.worxbend.tck

/** A renderer-agnostic mirror of each module's `Configuration`.
  *
  * The kit cannot name `com.worxbend.describo.Configuration` or `com.worxbend.dscrbo.Configuration` without depending
  * on a renderer, so it carries its own record with the same sixteen fields and the same defaults. Each module's
  * adapter translates this into its own `Configuration` in one obvious line per field, and a compile error there is
  * exactly the signal you want if the two records ever drift apart.
  *
  * Field meanings are documented once, on each module's `Configuration`; they are intentionally not restated here.
  */
final case class TckConfiguration(
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

object TckConfiguration:

  /** The baseline every fixture is specified against unless it names another. */
  val default: TckConfiguration = TckConfiguration()

  /** Forces one line regardless of field count, which is what most fixtures want. */
  val flat: TckConfiguration = TckConfiguration(multilineIfFieldsAreGreaterOrEqual = -1)

  /** Forces the multiline layout regardless of field count. */
  val expanded: TckConfiguration = TckConfiguration(multiline = true)
