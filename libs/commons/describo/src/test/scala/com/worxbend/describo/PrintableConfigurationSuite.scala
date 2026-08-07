package com.worxbend.describo

import com.worxbend.describo.annotations.Excluded

import org.scalatest.funsuite.AnyFunSuite

final case class ConfigPair(alpha: Int, beta: String) derives Printable

final case class ConfigTriple(alpha: Int, beta: Int, gamma: Int) derives Printable

final case class ConfigAllExcluded(@Excluded alpha: Int, @Excluded beta: Int) derives Printable

class PrintableConfigurationSuite extends AnyFunSuite:

  private val pair: ConfigPair = ConfigPair(1, "x")

  private def render(conf: Configuration): String = summon[Printable[ConfigPair]].asString(pair)(using conf)

  private def renderTriple(conf: Configuration): String =
    summon[Printable[ConfigTriple]].asString(ConfigTriple(1, 2, 3))(using conf)

  test("the default configuration renders field names on a single line"):
    assert(summon[Printable[ConfigPair]].asString(pair) == "ConfigPair(alpha = 1, beta = \"x\")")

  test("Configuration.default is the configuration used when no argument is given"):
    assert(summon[Printable[ConfigPair]].asString(pair) == render(Configuration.default))

  test("useFieldNames = false drops the field names"):
    assert(render(Configuration(useFieldNames = false)) == "ConfigPair(1, \"x\")")

  test("useTypeNames = true adds the declared field types"):
    assert(render(Configuration(useTypeNames = true)) == "ConfigPair(alpha: Int = 1, beta: String = \"x\")")

  test("useFieldNames = false suppresses the type names as well"):
    assert(render(Configuration(useFieldNames = false, useTypeNames = true)) == "ConfigPair(1, \"x\")")

  test("fullyQualifiedClassName = true with shortPackagePrefix = false prints the whole package"):
    val conf = Configuration(fullyQualifiedClassName = true, shortPackagePrefix = false)
    assert(render(conf) == "com.worxbend.describo.ConfigPair(alpha = 1, beta = \"x\")")

  test("fullyQualifiedClassName = true with shortPackagePrefix = true compresses the package"):
    val conf = Configuration(fullyQualifiedClassName = true, shortPackagePrefix = true)
    assert(render(conf) == "c.w.d.ConfigPair(alpha = 1, beta = \"x\")")

  test("fullyQualifiedClassName also applies to the declared field types"):
    val conf = Configuration(useTypeNames = true, fullyQualifiedClassName = true, shortPackagePrefix = false)
    assert(render(conf) == "com.worxbend.describo.ConfigPair(alpha: scala.Int = 1, beta: java.lang.String = \"x\")")

  test("fieldsSeparator is used verbatim on a single line"):
    assert(render(Configuration(fieldsSeparator = " | ")) == "ConfigPair(alpha = 1 | beta = \"x\")")

  test("fieldsSeparator without surrounding spaces is not padded"):
    assert(render(Configuration(fieldsSeparator = ";")) == "ConfigPair(alpha = 1;beta = \"x\")")

  test("fieldNamePrefix and fieldNameSuffix wrap the field name"):
    val conf = Configuration(fieldNamePrefix = "<", fieldNameSuffix = ">")
    assert(render(conf) == "ConfigPair(<alpha> = 1, <beta> = \"x\")")

  test("fieldNameAndValueSeparator replaces the equals sign"):
    assert(render(Configuration(fieldNameAndValueSeparator = " -> ")) == "ConfigPair(alpha -> 1, beta -> \"x\")")

  test("fieldNameAndTypeNameSeparator replaces the colon"):
    val conf = Configuration(useTypeNames = true, fieldNameAndTypeNameSeparator = "@")
    assert(render(conf) == "ConfigPair(alpha@Int = 1, beta@String = \"x\")")

  test("typeNamePrefix and typeNameSuffix wrap the type name"):
    val conf = Configuration(useTypeNames = true, typeNamePrefix = "[", typeNameSuffix = "]")
    assert(render(conf) == "ConfigPair(alpha: [Int] = 1, beta: [String] = \"x\")")

  test("valuePrefix and valueSuffix wrap the value"):
    val conf = Configuration(valuePrefix = "(", valueSuffix = ")")
    assert(render(conf) == "ConfigPair(alpha = (1), beta = (\"x\"))")

  test("multiline = true renders one field per line even below the threshold"):
    val expected =
      """|ConfigPair(
         |  alpha = 1,
         |  beta = "x"
         |)""".stripMargin
    assert(render(Configuration(multiline = true)) == expected)

  test("multilineIndent controls the per-field indentation"):
    val expected =
      """|ConfigPair(
         |....alpha = 1,
         |....beta = "x"
         |)""".stripMargin
    assert(render(Configuration(multiline = true, multilineIndent = "....")) == expected)

  test("multiline strips only trailing whitespace from the fields separator"):
    val expected =
      """|ConfigPair(
         |  alpha = 1 |
         |  beta = "x"
         |)""".stripMargin
    assert(render(Configuration(multiline = true, fieldsSeparator = " | ")) == expected)

  test("multilineIfFieldsAreGreaterOrEqual switches to multiline at exactly the threshold"):
    val expected =
      """|ConfigTriple(
         |  alpha = 1,
         |  beta = 2,
         |  gamma = 3
         |)""".stripMargin
    assert(renderTriple(Configuration(multilineIfFieldsAreGreaterOrEqual = 3)) == expected)

  test("multilineIfFieldsAreGreaterOrEqual stays single line one field below the threshold"):
    val actual = renderTriple(Configuration(multilineIfFieldsAreGreaterOrEqual = 4))
    assert(actual == "ConfigTriple(alpha = 1, beta = 2, gamma = 3)")

  test("multilineIfFieldsAreGreaterOrEqual = 0 disables the threshold"):
    val actual = renderTriple(Configuration(multilineIfFieldsAreGreaterOrEqual = 0))
    assert(actual == "ConfigTriple(alpha = 1, beta = 2, gamma = 3)")

  test("a negative multilineIfFieldsAreGreaterOrEqual disables the threshold"):
    val actual = renderTriple(Configuration(multilineIfFieldsAreGreaterOrEqual = -1))
    assert(actual == "ConfigTriple(alpha = 1, beta = 2, gamma = 3)")

  test("the disabled threshold does not override an explicit multiline = true"):
    val expected =
      """|ConfigTriple(
         |  alpha = 1,
         |  beta = 2,
         |  gamma = 3
         |)""".stripMargin
    assert(renderTriple(Configuration(multiline = true, multilineIfFieldsAreGreaterOrEqual = -1)) == expected)

  test("a type whose fields are all excluded stays on one line under multiline = true"):
    val actual =
      summon[Printable[ConfigAllExcluded]].asString(ConfigAllExcluded(1, 2))(using Configuration(multiline = true))
    assert(actual == "ConfigAllExcluded()")

  test("the fields separator is unused when no field is rendered"):
    val conf   = Configuration(multiline = true, fieldsSeparator = " | ")
    val actual = summon[Printable[ConfigAllExcluded]].asString(ConfigAllExcluded(1, 2))(using conf)
    assert(actual == "ConfigAllExcluded()")
