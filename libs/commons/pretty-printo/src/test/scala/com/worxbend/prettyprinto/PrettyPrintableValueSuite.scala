package com.worxbend.prettyprinto

import org.scalatest.funsuite.AnyFunSuite

final case class ValString(value: String) derives PrettyPrintable

final case class ValChar(value: Char) derives PrettyPrintable

final case class ValNumbers(
    int:        Int,
    long:       Long,
    short:      Short,
    byte:       Byte,
    double:     Double,
    float:      Float,
    bigInt:     BigInt,
    bigDecimal: BigDecimal,
) derives PrettyPrintable

final case class ValBoolean(value: Boolean) derives PrettyPrintable

final case class ValTemporal(
    date:     java.time.LocalDate,
    time:     java.time.LocalTime,
    instant:  java.time.Instant,
    duration: java.time.Duration,
    period:   java.time.Period,
) derives PrettyPrintable

final case class ValStrings(values: List[String]) derives PrettyPrintable

final case class ValBoxedInt(value: java.lang.Integer) derives PrettyPrintable

final case class ValBoxedChar(value: java.lang.Character) derives PrettyPrintable

final case class ValZoned(
    zoned:  java.time.ZonedDateTime,
    offset: java.time.OffsetDateTime,
    time:   java.time.OffsetTime,
) derives PrettyPrintable

class PrettyPrintableValueSuite extends AnyFunSuite:

  private def string(raw: String): String = summon[PrettyPrintable[ValString]].asString(ValString(raw))

  test("a plain string is wrapped in double quotes"):
    assert(string("abc") == "ValString(value = \"abc\")")

  test("an empty string renders as a pair of quotes"):
    assert(string("") == "ValString(value = \"\")")

  test("a double quote inside a string is escaped"):
    assert(string("a\"b") == "ValString(value = \"a\\\"b\")")

  test("a backslash inside a string is escaped"):
    assert(string("a\\b") == "ValString(value = \"a\\\\b\")")

  test("a backslash before a quote is escaped exactly once"):
    assert(string("\\\"") == "ValString(value = \"\\\\\\\"\")")

  test("a newline inside a string is escaped"):
    assert(string("a\nb") == "ValString(value = \"a\\nb\")")

  test("a carriage return inside a string is escaped"):
    assert(string("a\rb") == "ValString(value = \"a\\rb\")")

  test("a tab inside a string is escaped"):
    assert(string("a\tb") == "ValString(value = \"a\\tb\")")

  test("commas and parentheses inside a string are left alone"):
    assert(string("a, (b)") == "ValString(value = \"a, (b)\")")

  test("a string element inside a collection is escaped too"):
    val actual = summon[PrettyPrintable[ValStrings]].asString(ValStrings(List("a\"b")))
    assert(actual == "ValStrings(values = [\"a\\\"b\"])")

  test("a Char is wrapped in single quotes"):
    assert(summon[PrettyPrintable[ValChar]].asString(ValChar('c')) == "ValChar(value = 'c')")

  test("a single quote Char is escaped"):
    assert(summon[PrettyPrintable[ValChar]].asString(ValChar('\'')) == "ValChar(value = '\\'')")

  test("a newline Char is escaped"):
    assert(summon[PrettyPrintable[ValChar]].asString(ValChar('\n')) == "ValChar(value = '\\n')")

  test("numeric values render without quotes"):
    val value    = ValNumbers(1, 2L, 3.toShort, 4.toByte, 5.5d, 6.5f, BigInt(7), BigDecimal("8.50"))
    val expected =
      "ValNumbers(int = 1, long = 2, short = 3, byte = 4, double = 5.5, float = 6.5, bigInt = 7, bigDecimal = 8.50)"
    val conf     = Configuration(multilineIfFieldsAreGreaterOrEqual = 0)
    assert(summon[PrettyPrintable[ValNumbers]].asString(value)(using conf) == expected)

  test("numeric fields print their declared Scala types, not their boxed JVM classes"):
    val value = ValNumbers(1, 2L, 3.toShort, 4.toByte, 5.5d, 6.5f, BigInt(7), BigDecimal("8.50"))
    val conf  = Configuration(useTypeNames = true, multilineIfFieldsAreGreaterOrEqual = 0)
    val types = summon[PrettyPrintable[ValNumbers]].asString(value)(using conf)
    assert(
      types == "ValNumbers(int: Int = 1, long: Long = 2, short: Short = 3, byte: Byte = 4, double: Double = 5.5, " +
        "float: Float = 6.5, bigInt: BigInt = 7, bigDecimal: BigDecimal = 8.50)"
    )

  test("a Boolean renders unquoted"):
    assert(summon[PrettyPrintable[ValBoolean]].asString(ValBoolean(true)) == "ValBoolean(value = true)")

  test("java.time values render through their own toString"):
    val value    = ValTemporal(
      date = java.time.LocalDate.parse("2023-01-01"),
      time = java.time.LocalTime.parse("10:15:30"),
      instant = java.time.Instant.parse("2023-01-01T00:00:00Z"),
      duration = java.time.Duration.ofMinutes(5),
      period = java.time.Period.ofDays(3),
    )
    val expected =
      "ValTemporal(date = 2023-01-01, time = 10:15:30, instant = 2023-01-01T00:00:00Z, duration = PT5M, period = P3D)"
    val conf     = Configuration(multilineIfFieldsAreGreaterOrEqual = 0)
    assert(summon[PrettyPrintable[ValTemporal]].asString(value)(using conf) == expected)

  test("a boxed java.lang.Integer renders as its number"):
    assert(summon[PrettyPrintable[ValBoxedInt]].asString(ValBoxedInt(7)) == "ValBoxedInt(value = 7)")

  test("a boxed java.lang.Integer field prints Integer as its declared type"):
    val conf = Configuration(useTypeNames = true)
    assert(summon[PrettyPrintable[ValBoxedInt]].asString(ValBoxedInt(7))(using
      conf) == "ValBoxedInt(value: Integer = 7)")

  test("a boxed java.lang.Character is quoted exactly like a Char"):
    assert(summon[PrettyPrintable[ValBoxedChar]].asString(ValBoxedChar('c')) == "ValBoxedChar(value = 'c')")

  test("a boxed java.lang.Character is escaped exactly like a Char"):
    assert(summon[PrettyPrintable[ValBoxedChar]].asString(ValBoxedChar('\'')) == "ValBoxedChar(value = '\\'')")

  test("zoned and offset java.time values render through their own toString"):
    val value    = ValZoned(
      zoned = java.time.ZonedDateTime.parse("2023-01-01T00:00:00Z[UTC]"),
      offset = java.time.OffsetDateTime.parse("2023-01-01T00:00:00+01:00"),
      time = java.time.OffsetTime.parse("10:15:30+01:00"),
    )
    val expected =
      "ValZoned(zoned = 2023-01-01T00:00Z[UTC], offset = 2023-01-01T00:00+01:00, time = 10:15:30+01:00)"
    assert(summon[PrettyPrintable[ValZoned]].asString(value) == expected)

  test("zoned and offset java.time fields print their declared types"):
    val value    = ValZoned(
      zoned = java.time.ZonedDateTime.parse("2023-01-01T00:00:00Z[UTC]"),
      offset = java.time.OffsetDateTime.parse("2023-01-01T00:00:00+01:00"),
      time = java.time.OffsetTime.parse("10:15:30+01:00"),
    )
    val expected =
      "ValZoned(zoned: ZonedDateTime = 2023-01-01T00:00Z[UTC], offset: OffsetDateTime = 2023-01-01T00:00+01:00, " +
        "time: OffsetTime = 10:15:30+01:00)"
    assert(summon[PrettyPrintable[ValZoned]].asString(value)(using Configuration(useTypeNames = true)) == expected)

  test("java.time fields print their declared types"):
    val value = ValTemporal(
      date = java.time.LocalDate.parse("2023-01-01"),
      time = java.time.LocalTime.parse("10:15:30"),
      instant = java.time.Instant.parse("2023-01-01T00:00:00Z"),
      duration = java.time.Duration.ofMinutes(5),
      period = java.time.Period.ofDays(3),
    )
    val conf  = Configuration(useTypeNames = true, multilineIfFieldsAreGreaterOrEqual = 0)
    assert(summon[PrettyPrintable[ValTemporal]].asString(value)(using conf).contains("date: LocalDate = 2023-01-01"))
