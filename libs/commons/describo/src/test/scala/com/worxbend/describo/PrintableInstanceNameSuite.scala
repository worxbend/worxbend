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
  * `TypeRepr.of[X].dealias.typeSymbol.fullName` with a trailing `$` stripped, which is also what the sibling `dscrbo`
  * macro computes; the table below is describo's side of that contract.
  *
  * The rows are data, but each becomes its own scalatest case, so a break names the one instance that drifted instead
  * of reporting a single failure for the whole table.
  */
class PrintableInstanceNameSuite extends AnyFunSuite:

  private def check(label: String, actual: PrintedType, simple: String, qualified: String): Unit =
    test(s"the built-in instance for $label declares $simple / $qualified"):
      assert(actual == PrintedType(simple, qualified))

  check("String", summon[Printable[String]].printedType, "String", "java.lang.String")
  check("Char", summon[Printable[Char]].printedType, "Char", "scala.Char")
  check("Int", summon[Printable[Int]].printedType, "Int", "scala.Int")
  check("Long", summon[Printable[Long]].printedType, "Long", "scala.Long")
  check("Short", summon[Printable[Short]].printedType, "Short", "scala.Short")
  check("Byte", summon[Printable[Byte]].printedType, "Byte", "scala.Byte")
  check("Double", summon[Printable[Double]].printedType, "Double", "scala.Double")
  check("Float", summon[Printable[Float]].printedType, "Float", "scala.Float")
  check("Boolean", summon[Printable[Boolean]].printedType, "Boolean", "scala.Boolean")
  check("BigInt", summon[Printable[BigInt]].printedType, "BigInt", "scala.math.BigInt")
  check("BigDecimal", summon[Printable[BigDecimal]].printedType, "BigDecimal", "scala.math.BigDecimal")
  check("java.lang.Integer", summon[Printable[java.lang.Integer]].printedType, "Integer", "java.lang.Integer")
  check("java.lang.Character", summon[Printable[java.lang.Character]].printedType, "Character", "java.lang.Character")

  check("LocalDate", summon[Printable[LocalDate]].printedType, "LocalDate", "java.time.LocalDate")
  check("LocalTime", summon[Printable[LocalTime]].printedType, "LocalTime", "java.time.LocalTime")
  check("Instant", summon[Printable[Instant]].printedType, "Instant", "java.time.Instant")
  check("Duration", summon[Printable[Duration]].printedType, "Duration", "java.time.Duration")
  check("Period", summon[Printable[Period]].printedType, "Period", "java.time.Period")
  check("ZonedDateTime", summon[Printable[ZonedDateTime]].printedType, "ZonedDateTime", "java.time.ZonedDateTime")
  check("OffsetDateTime", summon[Printable[OffsetDateTime]].printedType, "OffsetDateTime", "java.time.OffsetDateTime")
  check("OffsetTime", summon[Printable[OffsetTime]].printedType, "OffsetTime", "java.time.OffsetTime")

  check("Option", summon[Printable[Option[Int]]].printedType, "Option", "scala.Option")
  check("Iterable", summon[Printable[Iterable[Int]]].printedType, "Iterable", "scala.collection.Iterable")
  check("Seq", summon[Printable[Seq[Int]]].printedType, "Seq", "scala.collection.immutable.Seq")

  check(
    "IndexedSeq",
    summon[Printable[IndexedSeq[Int]]].printedType,
    "IndexedSeq",
    "scala.collection.immutable.IndexedSeq",
  )

  check("List", summon[Printable[List[Int]]].printedType, "List", "scala.collection.immutable.List")
  check("Vector", summon[Printable[Vector[Int]]].printedType, "Vector", "scala.collection.immutable.Vector")
  check("Set", summon[Printable[Set[Int]]].printedType, "Set", "scala.collection.immutable.Set")
  check("Array", summon[Printable[Array[Int]]].printedType, "Array", "scala.Array")
  check("Map", summon[Printable[Map[String, Int]]].printedType, "Map", "scala.collection.immutable.Map")

  check("java.util.List", summon[Printable[java.util.List[Int]]].printedType, "List", "java.util.List")

  check(
    "java.util.ArrayList",
    summon[Printable[java.util.ArrayList[Int]]].printedType,
    "ArrayList",
    "java.util.ArrayList",
  )

  check(
    "java.util.LinkedList",
    summon[Printable[java.util.LinkedList[Int]]].printedType,
    "LinkedList",
    "java.util.LinkedList",
  )

  check("java.util.Set", summon[Printable[java.util.Set[Int]]].printedType, "Set", "java.util.Set")
  check("java.util.HashSet", summon[Printable[java.util.HashSet[Int]]].printedType, "HashSet", "java.util.HashSet")
  check("java.util.Map", summon[Printable[java.util.Map[String, Int]]].printedType, "Map", "java.util.Map")

  check(
    "java.util.HashMap",
    summon[Printable[java.util.HashMap[String, Int]]].printedType,
    "HashMap",
    "java.util.HashMap",
  )
