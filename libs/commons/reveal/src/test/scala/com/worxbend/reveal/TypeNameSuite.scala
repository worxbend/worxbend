package com.worxbend.reveal

import java.time.Instant
import java.time.LocalDate

import org.scalatest.funsuite.AnyFunSuite

final case class NamBox[A](value: A)

object NamOuter:
  final case class Inner(value: Int) derives PrettyPrintable

final case class NamDeclared(
    text:    String,
    number:  Int,
    amount:  BigDecimal,
    day:     LocalDate,
    at:      Instant,
    items:   List[String],
    lookup:  Map[String, String],
    perhaps: Option[String],
) derives PrettyPrintable

/** How declared type names are spelled, and how qualified names are compressed. */
final class TypeNameSuite extends AnyFunSuite:

  private val typed: Configuration =
    Configuration(multilineIfFieldsAreGreaterOrEqual = -1, useTypeNames = true, useFieldNames = true)

  private val declared: NamDeclared =
    NamDeclared(
      text = "t",
      number = 1,
      amount = BigDecimal("1.5"),
      day = LocalDate.parse("2023-01-01"),
      at = Instant.parse("2023-01-01T00:00:00Z"),
      items = List("a"),
      lookup = Map("k" -> "v"),
      perhaps = Some("p"),
    )

  test("simple declared type names never leak a runtime class"):
    assert(
      PrettyPrintable[NamDeclared].describe(declared)(using typed) ==
        "NamDeclared(text: String = \"t\", number: Int = 1, amount: BigDecimal = 1.5, day: LocalDate = 2023-01-01, " +
        "at: Instant = 2023-01-01T00:00:00Z, items: List = [\"a\"], lookup: Map = [\"k\" -> \"v\"], " +
        "perhaps: Option = Some(\"p\"))"
    )

  test("qualified declared type names follow the dealiased type symbol"):
    val configuration = typed.copy(fullyQualifiedClassName = true, shortPackagePrefix = false)
    assert(
      PrettyPrintable[NamDeclared].describe(declared)(using configuration) ==
        "com.worxbend.reveal.NamDeclared(text: java.lang.String = \"t\", number: scala.Int = 1, " +
        "amount: scala.math.BigDecimal = 1.5, day: java.time.LocalDate = 2023-01-01, " +
        "at: java.time.Instant = 2023-01-01T00:00:00Z, items: scala.collection.immutable.List = [\"a\"], " +
        "lookup: scala.collection.immutable.Map = [\"k\" -> \"v\"], perhaps: scala.Option = Some(\"p\"))"
    )

  test("a type nested in an object keeps the enclosing object in its qualified name"):
    val configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = -1, fullyQualifiedClassName = true)
    assert(
      PrettyPrintable[NamOuter.Inner].describe(NamOuter.Inner(1))(using configuration) ==
        "c.w.r.NamOuter.Inner(value = 1)"
    )

  test("a generic case class renders its instantiated field types"):
    val configuration = typed
    assert(
      PrettyPrintable.derived[NamBox[String]].describe(NamBox("a"))(using configuration) ==
        "NamBox(value: String = \"a\")"
    )

  test("package compression shortens leading lowercase segments only"):
    assert(Rendering.compressPackages("com.worxbend.reveal.Fixture") == "c.w.r.Fixture")

  test("package compression leaves an unqualified name alone, with no leading dot"):
    assert(Rendering.compressPackages("Fixture") == "Fixture")

  test("package compression stops at the first capitalised segment"):
    assert(Rendering.compressPackages("com.worxbend.reveal.Fixtures.Inner") == "c.w.r.Fixtures.Inner")
