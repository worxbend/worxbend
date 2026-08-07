package com.worxbend.describo

import org.scalatest.funsuite.AnyFunSuite

final case class CollList(values: List[String]) derives Printable

final case class CollVector(values: Vector[Int]) derives Printable

final case class CollSet(values: Set[Int]) derives Printable

final case class CollSeq(values: Seq[String]) derives Printable

final case class CollIndexedSeq(values: IndexedSeq[Int]) derives Printable

final case class CollIterable(values: Iterable[Int]) derives Printable

final case class CollArray(values: Array[Int]) derives Printable

final case class CollMap(values: Map[String, String]) derives Printable

final case class CollMapOfInts(values: Map[String, Int]) derives Printable

final case class CollNestedList(values: List[List[String]]) derives Printable

final case class CollJavaList(values: java.util.List[String]) derives Printable

final case class CollJavaArrayList(values: java.util.ArrayList[String]) derives Printable

final case class CollJavaLinkedList(values: java.util.LinkedList[String]) derives Printable

final case class CollJavaSet(values: java.util.Set[String]) derives Printable

final case class CollJavaHashSet(values: java.util.HashSet[String]) derives Printable

final case class CollJavaMap(values: java.util.Map[String, String]) derives Printable

final case class CollJavaHashMap(values: java.util.HashMap[String, String]) derives Printable

final case class CollOptionString(value: Option[String]) derives Printable

final case class CollOptionNested(value: Option[CollList]) derives Printable

class PrintableCollectionsSuite extends AnyFunSuite:

  test("a List renders as bracketed, quoted elements"):
    assert(summon[Printable[CollList]].asString(CollList(List("a", "b"))) == "CollList(values = [\"a\", \"b\"])")

  test("an empty List renders as empty brackets"):
    assert(summon[Printable[CollList]].asString(CollList(Nil)) == "CollList(values = [])")

  test("a Vector renders as bracketed elements"):
    assert(summon[Printable[CollVector]].asString(CollVector(Vector(1, 2))) == "CollVector(values = [1, 2])")

  test("a Set renders as bracketed elements"):
    assert(summon[Printable[CollSet]].asString(CollSet(Set(1, 2))) == "CollSet(values = [1, 2])")

  test("a Seq renders as bracketed elements"):
    assert(summon[Printable[CollSeq]].asString(CollSeq(Seq("a"))) == "CollSeq(values = [\"a\"])")

  test("an IndexedSeq renders as bracketed elements"):
    val actual = summon[Printable[CollIndexedSeq]].asString(CollIndexedSeq(IndexedSeq(7)))
    assert(actual == "CollIndexedSeq(values = [7])")

  test("an Iterable renders as bracketed elements"):
    assert(summon[Printable[CollIterable]].asString(CollIterable(Iterable(9))) == "CollIterable(values = [9])")

  test("an Array renders as bracketed elements"):
    assert(summon[Printable[CollArray]].asString(CollArray(Array(1, 2))) == "CollArray(values = [1, 2])")

  test("a List of Lists renders nested brackets"):
    val actual = summon[Printable[CollNestedList]].asString(CollNestedList(List(List("a"), List("b", "c"))))
    assert(actual == "CollNestedList(values = [[\"a\"], [\"b\", \"c\"]])")

  test("a Map renders arrow-separated entries in brackets"):
    val actual = summon[Printable[CollMap]].asString(CollMap(Map("k1" -> "v1", "k2" -> "v2")))
    assert(actual == "CollMap(values = [\"k1\" -> \"v1\", \"k2\" -> \"v2\"])")

  test("a Map renders its values by the same rules as any other value"):
    val actual = summon[Printable[CollMapOfInts]].asString(CollMapOfInts(Map("k" -> 1)))
    assert(actual == "CollMapOfInts(values = [\"k\" -> 1])")

  test("an empty Map renders as empty brackets"):
    assert(summon[Printable[CollMap]].asString(CollMap(Map.empty)) == "CollMap(values = [])")

  test("a java.util.List renders as bracketed elements"):
    val actual = summon[Printable[CollJavaList]].asString(CollJavaList(java.util.List.of("a", "b")))
    assert(actual == "CollJavaList(values = [\"a\", \"b\"])")

  test("a java.util.ArrayList renders as bracketed elements"):
    val values = java.util.ArrayList[String]()
    values.add("a")
    val actual = summon[Printable[CollJavaArrayList]].asString(CollJavaArrayList(values))
    assert(actual == "CollJavaArrayList(values = [\"a\"])")

  test("a java.util.LinkedList renders as bracketed elements"):
    val values = java.util.LinkedList[String]()
    values.add("a")
    val actual = summon[Printable[CollJavaLinkedList]].asString(CollJavaLinkedList(values))
    assert(actual == "CollJavaLinkedList(values = [\"a\"])")

  test("a java.util.Set renders as bracketed elements"):
    val actual = summon[Printable[CollJavaSet]].asString(CollJavaSet(java.util.Set.of("a")))
    assert(actual == "CollJavaSet(values = [\"a\"])")

  test("a java.util.HashSet renders as bracketed elements"):
    val values = java.util.HashSet[String]()
    values.add("a")
    val actual = summon[Printable[CollJavaHashSet]].asString(CollJavaHashSet(values))
    assert(actual == "CollJavaHashSet(values = [\"a\"])")

  test("a java.util.Map renders arrow-separated entries in brackets"):
    val actual = summon[Printable[CollJavaMap]].asString(CollJavaMap(java.util.Map.of("k", "v")))
    assert(actual == "CollJavaMap(values = [\"k\" -> \"v\"])")

  test("a java.util.HashMap renders in brackets, not braces"):
    val values = java.util.HashMap[String, String]()
    values.put("k", "v")
    val actual = summon[Printable[CollJavaHashMap]].asString(CollJavaHashMap(values))
    assert(actual == "CollJavaHashMap(values = [\"k\" -> \"v\"])")

  test("Some wraps the rendered payload, so a string payload stays quoted"):
    val actual = summon[Printable[CollOptionString]].asString(CollOptionString(Some("hi")))
    assert(actual == "CollOptionString(value = Some(\"hi\"))")

  test("None renders as None"):
    assert(summon[Printable[CollOptionString]].asString(CollOptionString(None)) == "CollOptionString(value = None)")

  test("Some of a case class renders the nested case class"):
    val actual = summon[Printable[CollOptionNested]].asString(CollOptionNested(Some(CollList(List("a")))))
    assert(actual == "CollOptionNested(value = Some(CollList(values = [\"a\"])))")

  test("the element separator is not the fields separator"):
    val conf   = Configuration(fieldsSeparator = " | ")
    val actual = summon[Printable[CollList]].asString(CollList(List("a", "b")))(using conf)
    assert(actual == "CollList(values = [\"a\", \"b\"])")

  test("a List field prints List as its declared type, not its runtime class"):
    val actual = summon[Printable[CollList]].asString(CollList(List("a")))(using Configuration(useTypeNames = true))
    assert(actual == "CollList(values: List = [\"a\"])")

  test("a Map field prints Map as its declared type, not its runtime class"):
    val actual = summon[Printable[CollMap]].asString(CollMap(Map("k" -> "v")))(using Configuration(useTypeNames = true))
    assert(actual == "CollMap(values: Map = [\"k\" -> \"v\"])")

  test("an Option field prints Option as its declared type, not Some"):
    val conf   = Configuration(useTypeNames = true)
    val actual = summon[Printable[CollOptionString]].asString(CollOptionString(Some("hi")))(using conf)
    assert(actual == "CollOptionString(value: Option = Some(\"hi\"))")
