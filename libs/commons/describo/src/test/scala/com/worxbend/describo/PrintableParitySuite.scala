package com.worxbend.describo

import com.worxbend.describo.annotations.Redacted

import org.scalatest.funsuite.AnyFunSuite

/** The cross-module parity fixture.
  *
  * `com.worxbend.dscrbo.DescribeParitySuite` declares the same fourteen-field `TestedType`, populates it with the same
  * values, renders it under the same three configurations and asserts the same three strings, character for character.
  * That is the whole point of the fixture: if either module's rendering drifts, these three tests are the first to
  * break. Keep the type name, the field names, the annotations, the values and the expected strings in step with that
  * suite — a difference in any of them makes the comparison vacuous rather than making it fail.
  */
final case class TestedType(
    @transient username: String,
    @Redacted password:  String,
    fullName:            String,
    age:                 Int,
    isAdmin:             Boolean,
    isSuperUser:         Boolean,
    isDisabled:          Boolean,
    createdDate:         java.time.LocalDate,
    lastLogin:           java.time.Instant,
    accountBalance:      BigDecimal,
    roles:               List[String],
    metadata:            Map[String, String],
    isVerified:          Option[Boolean],
    optionalComment:     Option[String],
) derives Printable

object ParityFixtures:

  val tested: TestedType = TestedType(
    username = "test-username",
    password = "test-password",
    fullName = "Test User",
    age = 1,
    isAdmin = false,
    isSuperUser = true,
    isDisabled = false,
    createdDate = java.time.LocalDate.parse("2023-01-01"),
    lastLogin = java.time.Instant.parse("2023-01-01T00:00:00Z"),
    accountBalance = BigDecimal("1000.50"),
    roles = List("Admin", "User"),
    metadata = Map("key1" -> "value1", "key2" -> "value2"),
    isVerified = Some(true),
    optionalComment = Some("This is a test comment"),
  )

  val customConfiguration: Configuration = Configuration(
    multiline = true,
    useTypeNames = true,
    fieldsSeparator = ",      ",
    valueSuffix = "]",
    valuePrefix = "[",
    typeNamePrefix = "<",
    typeNameSuffix = ">",
    fieldNameAndTypeNameSeparator = ":::",
    fieldNameAndValueSeparator = " -- ",
  )

class PrintableParitySuite extends AnyFunSuite:

  test("the parity fixture renders multiline under the default configuration"):
    val expected =
      """|TestedType(
         |  password = <redacted>,
         |  fullName = "Test User",
         |  age = 1,
         |  isAdmin = false,
         |  isSuperUser = true,
         |  isDisabled = false,
         |  createdDate = 2023-01-01,
         |  lastLogin = 2023-01-01T00:00:00Z,
         |  accountBalance = 1000.50,
         |  roles = ["Admin", "User"],
         |  metadata = ["key1" -> "value1", "key2" -> "value2"],
         |  isVerified = Some(true),
         |  optionalComment = Some("This is a test comment")
         |)""".stripMargin
    assert(summon[Printable[TestedType]].asString(ParityFixtures.tested)(using Configuration.default) == expected)

  test("the parity fixture renders declared type names under the custom configuration"):
    val expected =
      """|TestedType(
         |  password:::<String> -- [<redacted>],
         |  fullName:::<String> -- ["Test User"],
         |  age:::<Int> -- [1],
         |  isAdmin:::<Boolean> -- [false],
         |  isSuperUser:::<Boolean> -- [true],
         |  isDisabled:::<Boolean> -- [false],
         |  createdDate:::<LocalDate> -- [2023-01-01],
         |  lastLogin:::<Instant> -- [2023-01-01T00:00:00Z],
         |  accountBalance:::<BigDecimal> -- [1000.50],
         |  roles:::<List> -- [["Admin", "User"]],
         |  metadata:::<Map> -- [["key1" -> "value1", "key2" -> "value2"]],
         |  isVerified:::<Option> -- [Some(true)],
         |  optionalComment:::<Option> -- [Some("This is a test comment")]
         |)""".stripMargin
    val actual   =
      summon[Printable[TestedType]].asString(ParityFixtures.tested)(using ParityFixtures.customConfiguration)
    assert(actual == expected)

  test("the parity fixture renders on one line when the multiline threshold is disabled"):
    val expected =
      "TestedType(password = <redacted>, fullName = \"Test User\", age = 1, isAdmin = false, isSuperUser = true, " +
        "isDisabled = false, createdDate = 2023-01-01, lastLogin = 2023-01-01T00:00:00Z, accountBalance = 1000.50, " +
        "roles = [\"Admin\", \"User\"], metadata = [\"key1\" -> \"value1\", \"key2\" -> \"value2\"], " +
        "isVerified = Some(true), optionalComment = Some(\"This is a test comment\"))"
    val actual   = summon[Printable[TestedType]]
      .asString(ParityFixtures.tested)(using Configuration(multilineIfFieldsAreGreaterOrEqual = -1))
    assert(actual == expected)
