package com.worxbend.describo

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Period
import java.time.ZonedDateTime

import org.scalatest.funsuite.AnyFunSuite

/** Pins the `PrintedType` data of every built-in instance, one test per instance.
  *
  * These names are hand-written string literals that only surface under `useTypeNames` and `fullyQualifiedClassName`,
  * so a typo in one of them would otherwise ship unnoticed. The canonical rule is
  * `TypeRepr.of[X].dealias.typeSymbol.fullName` with a trailing `$` stripped, which is also what the sibling `reveal`
  * macro computes; the table below is describo's side of that contract.
  *
  * The rows are data, but each becomes its own scalatest case, so a break names the one instance that drifted instead
  * of reporting a single failure for the whole table.
  */
class PrettyPrintableInstanceNameSuite extends AnyFunSuite:

  private def check(label: String, actual: PrintedType, simple: String, qualified: String): Unit =
    test(s"the built-in instance for $label declares $simple / $qualified"):
      assert(actual == PrintedType(simple, qualified))

  check("String", summon[PrettyPrintable[String]].printedType, "String", "java.lang.String")
  check("Char", summon[PrettyPrintable[Char]].printedType, "Char", "scala.Char")
  check("Int", summon[PrettyPrintable[Int]].printedType, "Int", "scala.Int")
  check("Long", summon[PrettyPrintable[Long]].printedType, "Long", "scala.Long")
  check("Short", summon[PrettyPrintable[Short]].printedType, "Short", "scala.Short")
  check("Byte", summon[PrettyPrintable[Byte]].printedType, "Byte", "scala.Byte")
  check("Double", summon[PrettyPrintable[Double]].printedType, "Double", "scala.Double")
  check("Float", summon[PrettyPrintable[Float]].printedType, "Float", "scala.Float")
  check("Boolean", summon[PrettyPrintable[Boolean]].printedType, "Boolean", "scala.Boolean")
  check("BigInt", summon[PrettyPrintable[BigInt]].printedType, "BigInt", "scala.math.BigInt")
  check("BigDecimal", summon[PrettyPrintable[BigDecimal]].printedType, "BigDecimal", "scala.math.BigDecimal")
  check("java.lang.Integer", summon[PrettyPrintable[java.lang.Integer]].printedType, "Integer", "java.lang.Integer")

  check(
    "java.lang.Character",
    summon[PrettyPrintable[java.lang.Character]].printedType,
    "Character",
    "java.lang.Character",
  )

  check("LocalDate", summon[PrettyPrintable[LocalDate]].printedType, "LocalDate", "java.time.LocalDate")
  check("LocalTime", summon[PrettyPrintable[LocalTime]].printedType, "LocalTime", "java.time.LocalTime")
  check("Instant", summon[PrettyPrintable[Instant]].printedType, "Instant", "java.time.Instant")
  check("Duration", summon[PrettyPrintable[Duration]].printedType, "Duration", "java.time.Duration")
  check("Period", summon[PrettyPrintable[Period]].printedType, "Period", "java.time.Period")
  check("ZonedDateTime", summon[PrettyPrintable[ZonedDateTime]].printedType, "ZonedDateTime", "java.time.ZonedDateTime")

  check(
    "OffsetDateTime",
    summon[PrettyPrintable[OffsetDateTime]].printedType,
    "OffsetDateTime",
    "java.time.OffsetDateTime",
  )

  check("OffsetTime", summon[PrettyPrintable[OffsetTime]].printedType, "OffsetTime", "java.time.OffsetTime")

  check("Option", summon[PrettyPrintable[Option[Int]]].printedType, "Option", "scala.Option")
  check("Iterable", summon[PrettyPrintable[Iterable[Int]]].printedType, "Iterable", "scala.collection.Iterable")
  check("Seq", summon[PrettyPrintable[Seq[Int]]].printedType, "Seq", "scala.collection.immutable.Seq")

  check(
    "IndexedSeq",
    summon[PrettyPrintable[IndexedSeq[Int]]].printedType,
    "IndexedSeq",
    "scala.collection.immutable.IndexedSeq",
  )

  check("List", summon[PrettyPrintable[List[Int]]].printedType, "List", "scala.collection.immutable.List")
  check("Vector", summon[PrettyPrintable[Vector[Int]]].printedType, "Vector", "scala.collection.immutable.Vector")
  check("Set", summon[PrettyPrintable[Set[Int]]].printedType, "Set", "scala.collection.immutable.Set")
  check("Array", summon[PrettyPrintable[Array[Int]]].printedType, "Array", "scala.Array")
  check("Map", summon[PrettyPrintable[Map[String, Int]]].printedType, "Map", "scala.collection.immutable.Map")

  check("java.util.List", summon[PrettyPrintable[java.util.List[Int]]].printedType, "List", "java.util.List")

  check(
    "java.util.ArrayList",
    summon[PrettyPrintable[java.util.ArrayList[Int]]].printedType,
    "ArrayList",
    "java.util.ArrayList",
  )

  check(
    "java.util.LinkedList",
    summon[PrettyPrintable[java.util.LinkedList[Int]]].printedType,
    "LinkedList",
    "java.util.LinkedList",
  )

  check("java.util.Set", summon[PrettyPrintable[java.util.Set[Int]]].printedType, "Set", "java.util.Set")

  check(
    "java.util.HashSet",
    summon[PrettyPrintable[java.util.HashSet[Int]]].printedType,
    "HashSet",
    "java.util.HashSet",
  )

  check("java.util.Map", summon[PrettyPrintable[java.util.Map[String, Int]]].printedType, "Map", "java.util.Map")

  check(
    "java.util.HashMap",
    summon[PrettyPrintable[java.util.HashMap[String, Int]]].printedType,
    "HashMap",
    "java.util.HashMap",
  )
