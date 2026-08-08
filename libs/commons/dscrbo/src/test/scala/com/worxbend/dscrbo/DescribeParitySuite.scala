package com.worxbend.dscrbo

import com.worxbend.dscrbo.annotations.Redacted

import java.time.Instant
import java.time.LocalDate

import org.scalatest.funsuite.AnyFunSuite

/** The cross-module parity fixture.
  *
  * The same type, the same values and the same three configurations exist in com.worxbend.describo's test suite with
  * byte-identical expectations. Any drift between the two modules breaks these three tests first.
  */
final case class TestedType(
    @transient username: String,
    @Redacted password:  String,
    fullName:            String,
    age:                 Int,
    isAdmin:             Boolean,
    isSuperUser:         Boolean,
    isDisabled:          Boolean,
    createdDate:         LocalDate,
    lastLogin:           Instant,
    accountBalance:      BigDecimal,
    roles:               List[String],
    metadata:            Map[String, String],
    isVerified:          Option[Boolean],
    optionalComment:     Option[String],
) derives Describe

final class DescribeParitySuite extends AnyFunSuite:

  private val instance: TestedType =
    TestedType(
      username = "test-username",
      password = "test-password",
      fullName = "Test User",
      age = 1,
      isAdmin = false,
      isSuperUser = true,
      isDisabled = false,
      createdDate = LocalDate.parse("2023-01-01"),
      lastLogin = Instant.parse("2023-01-01T00:00:00Z"),
      accountBalance = BigDecimal("1000.50"),
      roles = List("Admin", "User"),
      metadata = Map("key1" -> "value1", "key2" -> "value2"),
      isVerified = Some(true),
      optionalComment = Some("This is a test comment"),
    )

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
    assert(Describe[TestedType].describe(instance)(using Configuration.default) == expected)

  test("the parity fixture renders under the custom configuration"):
    val configuration =
      Configuration(
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
    val expected      =
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
    assert(Describe[TestedType].describe(instance)(using configuration) == expected)

  test("the parity fixture renders on one line when the multiline threshold is disabled"):
    val configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = -1)
    val expected      =
      "TestedType(password = <redacted>, fullName = \"Test User\", age = 1, isAdmin = false, isSuperUser = true, " +
        "isDisabled = false, createdDate = 2023-01-01, lastLogin = 2023-01-01T00:00:00Z, accountBalance = 1000.50, " +
        "roles = [\"Admin\", \"User\"], metadata = [\"key1\" -> \"value1\", \"key2\" -> \"value2\"], " +
        "isVerified = Some(true), optionalComment = Some(\"This is a test comment\"))"
    assert(Describe[TestedType].describe(instance)(using configuration) == expected)
