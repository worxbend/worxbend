package com.worxbend.dscrbo

import com.worxbend.dscrbo.annotations.Excluded

import org.scalatest.funsuite.AnyFunSuite

final case class CfgPair(alpha: String, beta: Int) derives Describe

final case class CfgFour(a: Int, b: Int, c: Int, d: Int) derives Describe

final case class CfgFive(a: Int, b: Int, c: Int, d: Int, e: Int) derives Describe

final case class CfgSixMostlyExcluded(
    a:           Int,
    b:           Int,
    c:           Int,
    d:           Int,
    @Excluded e: Int,
    @Excluded f: Int,
) derives Describe

final case class CfgAllExcluded(@Excluded only: String) derives Describe

final case class CfgNullable(value: String) derives Describe

/** Every configuration flag, one flag per test, always applied explicitly rather than through a mixin. */
final class ConfigurationSuite extends AnyFunSuite:

  private val flat: CfgPair = CfgPair("x", 1)

  private val singleLine: Configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = -1)

  private def render(configuration: Configuration): String = Describe[CfgPair].describe(flat)(using configuration)

  test("useFieldNames renders names by default"):
    assert(render(singleLine) == "CfgPair(alpha = \"x\", beta = 1)")

  test("useFieldNames = false drops names and type names"):
    assert(render(singleLine.copy(useFieldNames = false, useTypeNames = true)) == "CfgPair(\"x\", 1)")

  test("useTypeNames renders the declared type of each field"):
    assert(render(singleLine.copy(useTypeNames = true)) == "CfgPair(alpha: String = \"x\", beta: Int = 1)")

  test("fullyQualifiedClassName with shortPackagePrefix compresses leading package segments"):
    assert(
      render(singleLine.copy(fullyQualifiedClassName = true)) == "c.w.d.CfgPair(alpha = \"x\", beta = 1)"
    )

  test("fullyQualifiedClassName without shortPackagePrefix spells the whole name"):
    assert(
      render(singleLine.copy(fullyQualifiedClassName = true, shortPackagePrefix = false)) ==
        "com.worxbend.dscrbo.CfgPair(alpha = \"x\", beta = 1)"
    )

  test("fullyQualifiedClassName also qualifies field type names"):
    assert(
      render(singleLine.copy(fullyQualifiedClassName = true, shortPackagePrefix = false, useTypeNames = true)) ==
        "com.worxbend.dscrbo.CfgPair(alpha: java.lang.String = \"x\", beta: scala.Int = 1)"
    )

  test("shortPackagePrefix compresses field type names too"):
    assert(
      render(singleLine.copy(fullyQualifiedClassName = true, useTypeNames = true)) ==
        "c.w.d.CfgPair(alpha: j.l.String = \"x\", beta: s.Int = 1)"
    )

  test("fieldsSeparator is used verbatim on a single line"):
    assert(render(singleLine.copy(fieldsSeparator = " | ")) == "CfgPair(alpha = \"x\" | beta = 1)")

  test("fieldsSeparator keeps leading whitespace and drops trailing whitespace when multiline"):
    assert(
      render(singleLine.copy(fieldsSeparator = " | ", multiline = true)) ==
        "CfgPair(\n  alpha = \"x\" |\n  beta = 1\n)"
    )

  test("fieldNamePrefix and fieldNameSuffix wrap the field name"):
    assert(
      render(singleLine.copy(fieldNamePrefix = "<", fieldNameSuffix = ">")) ==
        "CfgPair(<alpha> = \"x\", <beta> = 1)"
    )

  test("fieldNameAndValueSeparator replaces the equals sign"):
    assert(render(singleLine.copy(fieldNameAndValueSeparator = " -> ")) == "CfgPair(alpha -> \"x\", beta -> 1)")

  test("fieldNameAndTypeNameSeparator sits between the name and the type"):
    assert(
      render(singleLine.copy(useTypeNames = true, fieldNameAndTypeNameSeparator = ":::")) ==
        "CfgPair(alpha:::String = \"x\", beta:::Int = 1)"
    )

  test("typeNamePrefix and typeNameSuffix wrap the type name"):
    assert(
      render(singleLine.copy(useTypeNames = true, typeNamePrefix = "<", typeNameSuffix = ">")) ==
        "CfgPair(alpha: <String> = \"x\", beta: <Int> = 1)"
    )

  test("valuePrefix and valueSuffix wrap every value"):
    assert(
      render(singleLine.copy(valuePrefix = "[", valueSuffix = "]")) == "CfgPair(alpha = [\"x\"], beta = [1])"
    )

  test("valuePrefix and valueSuffix also wrap a null value"):
    val configuration = singleLine.copy(valuePrefix = "[", valueSuffix = "]")
    assert(Describe[CfgNullable].describe(CfgNullable(null))(using configuration) == "CfgNullable(value = [null])")

  test("multiline forces the multiline layout below the threshold"):
    assert(render(singleLine.copy(multiline = true)) == "CfgPair(\n  alpha = \"x\",\n  beta = 1\n)")

  test("multilineIndent controls the per-field indentation"):
    assert(
      render(singleLine.copy(multiline = true, multilineIndent = "....")) ==
        "CfgPair(\n....alpha = \"x\",\n....beta = 1\n)"
    )

  test("multilineIfFieldsAreGreaterOrEqual triggers exactly at the threshold"):
    val configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = 5)
    assert(
      Describe[CfgFive].describe(CfgFive(1, 2, 3, 4, 5))(using configuration) ==
        "CfgFive(\n  a = 1,\n  b = 2,\n  c = 3,\n  d = 4,\n  e = 5\n)"
    )

  test("multilineIfFieldsAreGreaterOrEqual leaves one field below the threshold on a single line"):
    val configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = 5)
    assert(
      Describe[CfgFour].describe(CfgFour(1, 2, 3, 4))(using configuration) == "CfgFour(a = 1, b = 2, c = 3, d = 4)"
    )

  test("multilineIfFieldsAreGreaterOrEqual counts fields after exclusion"):
    val configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = 5)
    assert(
      Describe[CfgSixMostlyExcluded].describe(CfgSixMostlyExcluded(1, 2, 3, 4, 5, 6))(using configuration) ==
        "CfgSixMostlyExcluded(a = 1, b = 2, c = 3, d = 4)"
    )

  test("multilineIfFieldsAreGreaterOrEqual = 0 disables the threshold"):
    val configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = 0)
    assert(
      Describe[CfgFive].describe(CfgFive(1, 2, 3, 4, 5))(using configuration) ==
        "CfgFive(a = 1, b = 2, c = 3, d = 4, e = 5)"
    )

  test("a negative multilineIfFieldsAreGreaterOrEqual disables the threshold"):
    val configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = -1)
    assert(
      Describe[CfgFive].describe(CfgFive(1, 2, 3, 4, 5))(using configuration) ==
        "CfgFive(a = 1, b = 2, c = 3, d = 4, e = 5)"
    )

  test("a type with no rendered fields stays on one line even when multiline is requested"):
    val configuration = Configuration(multiline = true)
    assert(Describe[CfgAllExcluded].describe(CfgAllExcluded("s"))(using configuration) == "CfgAllExcluded()")

  test("Configuration.default is the same value as an unconfigured Configuration"):
    assert(Configuration.default == Configuration())
