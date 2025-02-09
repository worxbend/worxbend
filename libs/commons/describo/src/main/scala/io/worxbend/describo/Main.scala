package io.worxbend.describo

import io.worxbend.describo.Main.Foo.given
import io.worxbend.describo.Printable.Configuration
import io.worxbend.describo.Printable.annotations.Excluded
import io.worxbend.describo.Printable.annotations.Redacted

object Main:

  case class Foo(
      @transient string:  String,
      boolean:            Boolean,
      char:               Char,
      short:              Short,
      long:               Long,
      @Redacted password: String,
      @Excluded username: String,
      fullName:           String,
      age:                Int,
      isAdmin:            Boolean,
      isSuperUser:        Boolean,
      isDisabled:         Boolean,
      createdDate:        java.time.LocalDate,
      lastLogin:          java.time.Instant,
      accountBalance:     BigDecimal,
      roles:              List[String],
      metadata:           Map[String, String],
      isVerified:         Option[Boolean],
      optionalComment:    Option[String],
    ) extends AutoToString
      derives Printable

  case class Bar(
      string:   String,
      boolean:  Boolean,
      char:     Char,
      short:    Short,
      long:     Long,
      password: String,
      username: String,
    )

  object Foo:
    given configuration: Configuration = Configuration(useTypeNames = true)
  def main(args: Array[String]): Unit =
    println(
      Foo(
        string = "bar",
        boolean = true,
        char = 'c',
        short = 1,
        long = 1L,
        password = "<PASSWORD>",
        username = "foo",
        fullName = "Test User",
        age = 30,
        isAdmin = false,
        isSuperUser = false,
        isDisabled = false,
        createdDate = java
          .time
          .LocalDate
          .of(
            2023,
            1,
            1,
          ),
        lastLogin = java.time.Instant.now(),
        accountBalance = BigDecimal(1000.50),
        roles = List("USER", "TEST"),
        metadata = Map("key1" -> "value1", "key2" -> "value2"),
        isVerified = Some(true),
        optionalComment = Some("This is a test comment"),
      ))
