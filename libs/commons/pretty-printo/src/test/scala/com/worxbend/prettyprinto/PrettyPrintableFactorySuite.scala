package com.worxbend.prettyprinto

import scala.jdk.CollectionConverters.*

import java.util.UUID

import org.scalatest.funsuite.AnyFunSuite

/** Exercises the four public factories exactly as the README tells users to call them. The typeclass is invariant, so
  * every collection type without a built-in instance has to be written by hand; these are the instances that make that
  * a one-liner, so the README example has to keep compiling.
  */
given factoryUuid: PrettyPrintable[UUID] = PrettyPrintable.instance("UUID", "java.util.UUID")(_.toString)

given factoryTreeMap: PrettyPrintable[java.util.TreeMap[String, Int]] =
  PrettyPrintable.mapping("TreeMap", "java.util.TreeMap")(_.asScala.iterator)

given factoryArrayDeque: PrettyPrintable[java.util.ArrayDeque[String]] =
  PrettyPrintable.collection("ArrayDeque", "java.util.ArrayDeque")(_.asScala.iterator)

final case class FactoryUuidHolder(id: UUID) derives PrettyPrintable

final case class FactoryTreeMapHolder(values: java.util.TreeMap[String, Int]) derives PrettyPrintable

final case class FactoryArrayDequeHolder(values: java.util.ArrayDeque[String]) derives PrettyPrintable

class PrettyPrintableFactorySuite extends AnyFunSuite:

  private val uuid: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")

  private def treeMap: java.util.TreeMap[String, Int] =
    val values = java.util.TreeMap[String, Int]()
    values.put("a", 1)
    values.put("b", 2)
    values

  private def arrayDeque: java.util.ArrayDeque[String] =
    val values = java.util.ArrayDeque[String]()
    values.add("a")
    values.add("b")
    values

  test("PrettyPrintable.instance renders a type that ships no built-in instance"):
    val actual = summon[PrettyPrintable[FactoryUuidHolder]].asString(FactoryUuidHolder(uuid))
    assert(actual == "FactoryUuidHolder(id = 00000000-0000-0000-0000-000000000001)")

  test("PrettyPrintable.instance prints the declared type name it was given"):
    val actual =
      summon[PrettyPrintable[FactoryUuidHolder]].asString(FactoryUuidHolder(uuid))(using
        Configuration(useTypeNames = true))
    assert(actual.startsWith("FactoryUuidHolder(id: UUID = "))

  test("PrettyPrintable.mapping renders an unsupported map type as bracketed, arrow-separated entries"):
    val actual = summon[PrettyPrintable[FactoryTreeMapHolder]].asString(FactoryTreeMapHolder(treeMap))
    assert(actual == "FactoryTreeMapHolder(values = [\"a\" -> 1, \"b\" -> 2])")

  test("PrettyPrintable.mapping prints the declared type name it was given"):
    val actual = summon[PrettyPrintable[FactoryTreeMapHolder]].asString(FactoryTreeMapHolder(treeMap))(using
      Configuration(useTypeNames = true))
    assert(actual.startsWith("FactoryTreeMapHolder(values: TreeMap = "))

  test("PrettyPrintable.collection renders an unsupported collection type as bracketed elements"):
    val actual = summon[PrettyPrintable[FactoryArrayDequeHolder]].asString(FactoryArrayDequeHolder(arrayDeque))
    assert(actual == "FactoryArrayDequeHolder(values = [\"a\", \"b\"])")

  test("PrettyPrintable.collection prints the declared type name it was given"):
    val actual = summon[PrettyPrintable[FactoryArrayDequeHolder]].asString(FactoryArrayDequeHolder(arrayDeque))(using
      Configuration(useTypeNames = true))
    assert(actual == "FactoryArrayDequeHolder(values: ArrayDeque = [\"a\", \"b\"])")

  test("a null field of a hand-written collection instance still renders as null"):
    val actual = summon[PrettyPrintable[FactoryArrayDequeHolder]].asString(FactoryArrayDequeHolder(null))
    assert(actual == "FactoryArrayDequeHolder(values = null)")

  test("a hand-written instance inherits the element escaping rather than re-implementing it"):
    val values = java.util.ArrayDeque[String]()
    values.add("a\"b")
    val actual = summon[PrettyPrintable[FactoryArrayDequeHolder]].asString(FactoryArrayDequeHolder(values))
    assert(actual == "FactoryArrayDequeHolder(values = [\"a\\\"b\"])")
