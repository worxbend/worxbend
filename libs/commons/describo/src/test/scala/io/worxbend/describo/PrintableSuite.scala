package io.worxbend.describo

import io.worxbend.describo.Printable.Configuration
import io.worxbend.describo.Printable.annotations.Redacted

import org.scalatest.funsuite.AnyFunSuite

class PrintableSuite extends AnyFunSuite:

  test("should generate multiline string with redacted password and skipped username fields"):

    given configuration: Configuration = Configuration()

    case class TestedType(
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
      ) extends AutoToString
        derives Printable

    val testedInstance = TestedType(
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

    val actual   = testedInstance.toString
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

    assert(actual == expected)

  test(
    "should generate multiline string with redacted password and skipped username fields with custom configuration and annotation arguments"):

    given configuration: Configuration =
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

    case class TestedType(
        @transient username:                     String,
        @Redacted(replacement = "***") password: String,
        fullName:                                String,
        age:                                     Int,
        isAdmin:                                 Boolean,
        isSuperUser:                             Boolean,
        isDisabled:                              Boolean,
        createdDate:                             java.time.LocalDate,
        lastLogin:                               java.time.Instant,
        accountBalance:                          BigDecimal,
        roles:                                   List[String],
        metadata:                                Map[String, String],
        isVerified:                              Option[Boolean],
        optionalComment:                         Option[String],
      ) extends AutoToString
        derives Printable

    val testedInstance = TestedType(
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

    val actual   = testedInstance.toString
    val expected =
      """|TestedType(
         |  password:::<String> -- [***],
         |  fullName:::<String> -- ["Test User"],
         |  age:::<Integer> -- [1],
         |  isAdmin:::<Boolean> -- [false],
         |  isSuperUser:::<Boolean> -- [true],
         |  isDisabled:::<Boolean> -- [false],
         |  createdDate:::<LocalDate> -- [2023-01-01],
         |  lastLogin:::<Instant> -- [2023-01-01T00:00:00Z],
         |  accountBalance:::<BigDecimal> -- [1000.50],
         |  roles:::<$colon$colon> -- [["Admin", "User"]],
         |  metadata:::<Map2> -- [["key1" -> "value1", "key2" -> "value2"]],
         |  isVerified:::<Some> -- [Some(true)],
         |  optionalComment:::<Some> -- [Some("This is a test comment")]
         |)""".stripMargin

    assert(actual == expected)

  test("should generate single string with redacted password and skipped username fields"):

    given configuration: Configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = -1)

    case class TestedType(
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
      ) extends AutoToString
        derives Printable

    val testedInstance = TestedType(
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

    val actual   = testedInstance.toString
    val expected =
      "TestedType(password = <redacted>, fullName = \"Test User\", age = 1, isAdmin = false, isSuperUser = true, isDisabled = false, createdDate = 2023-01-01, lastLogin = 2023-01-01T00:00:00Z, accountBalance = 1000.50, roles = [\"Admin\", \"User\"], metadata = [\"key1\" -> \"value1\", \"key2\" -> \"value2\"], isVerified = Some(true), optionalComment = Some(\"This is a test comment\"))"
    assert(actual == expected)
