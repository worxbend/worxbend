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

/** Pins the `PrintedType` data of every built-in instance.
  *
  * These names are hand-written string literals that only surface under `useTypeNames` and `fullyQualifiedClassName`,
  * so a typo in one of them would otherwise ship unnoticed. The canonical rule is
  * `TypeRepr.of[X].dealias.typeSymbol.fullName` with a trailing `$` stripped, which is also what the sibling `dscrbo`
  * macro computes; the table below is describo's side of that contract.
  */
class PrintableInstanceNameSuite extends AnyFunSuite:

  private val declared: Vector[(PrintedType, PrintedType)] = Vector(
    summon[Printable[String]].printedType                         -> PrintedType("String", "java.lang.String"),
    summon[Printable[Char]].printedType                           -> PrintedType("Char", "scala.Char"),
    summon[Printable[Int]].printedType                            -> PrintedType("Int", "scala.Int"),
    summon[Printable[Long]].printedType                           -> PrintedType("Long", "scala.Long"),
    summon[Printable[Short]].printedType                          -> PrintedType("Short", "scala.Short"),
    summon[Printable[Byte]].printedType                           -> PrintedType("Byte", "scala.Byte"),
    summon[Printable[Double]].printedType                         -> PrintedType("Double", "scala.Double"),
    summon[Printable[Float]].printedType                          -> PrintedType("Float", "scala.Float"),
    summon[Printable[Boolean]].printedType                        -> PrintedType("Boolean", "scala.Boolean"),
    summon[Printable[BigInt]].printedType                         -> PrintedType("BigInt", "scala.math.BigInt"),
    summon[Printable[BigDecimal]].printedType                     -> PrintedType("BigDecimal", "scala.math.BigDecimal"),
    summon[Printable[java.lang.Integer]].printedType              -> PrintedType("Integer", "java.lang.Integer"),
    summon[Printable[java.lang.Character]].printedType            -> PrintedType("Character", "java.lang.Character"),
    summon[Printable[LocalDate]].printedType                      -> PrintedType("LocalDate", "java.time.LocalDate"),
    summon[Printable[LocalTime]].printedType                      -> PrintedType("LocalTime", "java.time.LocalTime"),
    summon[Printable[Instant]].printedType                        -> PrintedType("Instant", "java.time.Instant"),
    summon[Printable[Duration]].printedType                       -> PrintedType("Duration", "java.time.Duration"),
    summon[Printable[Period]].printedType                         -> PrintedType("Period", "java.time.Period"),
    summon[Printable[ZonedDateTime]].printedType                  ->
      PrintedType("ZonedDateTime", "java.time.ZonedDateTime"),
    summon[Printable[OffsetDateTime]].printedType                 ->
      PrintedType("OffsetDateTime", "java.time.OffsetDateTime"),
    summon[Printable[OffsetTime]].printedType                     -> PrintedType("OffsetTime", "java.time.OffsetTime"),
    summon[Printable[Option[Int]]].printedType                    -> PrintedType("Option", "scala.Option"),
    summon[Printable[Iterable[Int]]].printedType                  ->
      PrintedType("Iterable", "scala.collection.Iterable"),
    summon[Printable[Seq[Int]]].printedType                       ->
      PrintedType("Seq", "scala.collection.immutable.Seq"),
    summon[Printable[IndexedSeq[Int]]].printedType                ->
      PrintedType("IndexedSeq", "scala.collection.immutable.IndexedSeq"),
    summon[Printable[List[Int]]].printedType                      ->
      PrintedType("List", "scala.collection.immutable.List"),
    summon[Printable[Vector[Int]]].printedType                    ->
      PrintedType("Vector", "scala.collection.immutable.Vector"),
    summon[Printable[Set[Int]]].printedType                       ->
      PrintedType("Set", "scala.collection.immutable.Set"),
    summon[Printable[Array[Int]]].printedType                     -> PrintedType("Array", "scala.Array"),
    summon[Printable[Map[String, Int]]].printedType               ->
      PrintedType("Map", "scala.collection.immutable.Map"),
    summon[Printable[java.util.List[Int]]].printedType            -> PrintedType("List", "java.util.List"),
    summon[Printable[java.util.ArrayList[Int]]].printedType       -> PrintedType("ArrayList", "java.util.ArrayList"),
    summon[Printable[java.util.LinkedList[Int]]].printedType      -> PrintedType("LinkedList", "java.util.LinkedList"),
    summon[Printable[java.util.Set[Int]]].printedType             -> PrintedType("Set", "java.util.Set"),
    summon[Printable[java.util.HashSet[Int]]].printedType         -> PrintedType("HashSet", "java.util.HashSet"),
    summon[Printable[java.util.Map[String, Int]]].printedType     -> PrintedType("Map", "java.util.Map"),
    summon[Printable[java.util.HashMap[String, Int]]].printedType -> PrintedType("HashMap", "java.util.HashMap"),
  )

  test("every built-in instance carries the declared type name pair the README documents"):
    assert(declared.filterNot((actual, expected) => actual == expected) == Vector.empty)
