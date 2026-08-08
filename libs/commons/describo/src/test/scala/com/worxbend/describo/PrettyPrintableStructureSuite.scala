package com.worxbend.describo

import com.worxbend.describo.annotations.Excluded
import com.worxbend.describo.annotations.Redacted

import org.scalatest.funsuite.AnyFunSuite

final case class StructEmpty() derives PrettyPrintable

final case class StructSingle(only: Int) derives PrettyPrintable

case object StructMarker derives PrettyPrintable

final case class StructAllExcluded(@Excluded a: Int, @Excluded b: Int) derives PrettyPrintable

object StructNesting:

  final case class Level4(value: String) derives PrettyPrintable
  final case class Level3(level4: Level4) derives PrettyPrintable
  final case class Level2(level3: Level3) derives PrettyPrintable
  final case class Level1(level2: Level2) derives PrettyPrintable

sealed trait StructShape derives PrettyPrintable

object StructShape:

  case object Point                     extends StructShape
  final case class Circle(radius: Int)  extends StructShape
  final case class Rect(w: Int, h: Int) extends StructShape

enum StructColour derives PrettyPrintable:

  case Red
  case Custom(hex: String)

final case class StructShapeHolder(shape: StructShape) derives PrettyPrintable

final case class StructUserId(value: String) extends AnyVal

object StructUserId:

  given PrettyPrintable[StructUserId] =
    PrettyPrintable.valueClass[StructUserId, String]("StructUserId", "com.worxbend.describo.StructUserId")(_.value)

final case class StructTaggedId(@Excluded value: String) extends AnyVal

object StructTaggedId:

  given PrettyPrintable[StructTaggedId] =
    PrettyPrintable.valueClass[StructTaggedId, String](
      "StructTaggedId",
      "com.worxbend.describo.StructTaggedId",
    )(_.value)

final case class StructTaggedHolder(tag: StructTaggedId) derives PrettyPrintable

final case class StructWithValueClass(id: StructUserId, name: String) derives PrettyPrintable

final case class StructRedactedValueClass(@Redacted id: StructUserId) derives PrettyPrintable

final case class StructNode(label: String, next: Option[StructNode]) derives PrettyPrintable

class PrettyPrintableStructureSuite extends AnyFunSuite:

  test("an empty case class renders as the name followed by empty parentheses"):
    assert(summon[PrettyPrintable[StructEmpty]].asString(StructEmpty()) == "StructEmpty()")

  test("a single-field case class renders that one field"):
    assert(summon[PrettyPrintable[StructSingle]].asString(StructSingle(1)) == "StructSingle(only = 1)")

  test("a case class whose fields are all excluded renders as empty parentheses"):
    assert(summon[PrettyPrintable[StructAllExcluded]].asString(StructAllExcluded(1, 2)) == "StructAllExcluded()")

  test("a case object renders as the bare name without parentheses"):
    assert(summon[PrettyPrintable[StructMarker.type]].asString(StructMarker) == "StructMarker")

  test("a nested case class is rendered by the same machinery"):
    val value = StructNesting.Level3(StructNesting.Level4("x"))
    assert(summon[PrettyPrintable[StructNesting.Level3]].asString(value) == "Level3(level4 = Level4(value = \"x\"))")

  test("a four-level nesting renders on one line"):
    val value    = StructNesting.Level1(StructNesting.Level2(StructNesting.Level3(StructNesting.Level4("x"))))
    val expected = "Level1(level2 = Level2(level3 = Level3(level4 = Level4(value = \"x\"))))"
    assert(summon[PrettyPrintable[StructNesting.Level1]].asString(value)(using
      Configuration(multilineIfFieldsAreGreaterOrEqual = -1)) == expected)

  test("a sealed trait dispatches to the matching case class"):
    assert(summon[PrettyPrintable[StructShape]].asString(StructShape.Circle(2)) == "Circle(radius = 2)")

  test("a sealed trait dispatches to a case object as the bare name"):
    assert(summon[PrettyPrintable[StructShape]].asString(StructShape.Point) == "Point")

  test("a sealed trait subtype rendered through a field keeps its own name"):
    val actual = summon[PrettyPrintable[StructShapeHolder]].asString(StructShapeHolder(StructShape.Rect(1, 2)))
    assert(actual == "StructShapeHolder(shape = Rect(w = 1, h = 2))")

  test("a parameterless enum case renders as the bare name without parentheses"):
    assert(summon[PrettyPrintable[StructColour]].asString(StructColour.Red) == "Red")

  test("a parameterised enum case renders its fields"):
    assert(summon[PrettyPrintable[StructColour]].asString(StructColour.Custom("#fff")) == "Custom(hex = \"#fff\")")

  test("a value class renders as its payload"):
    assert(summon[PrettyPrintable[StructUserId]].asString(StructUserId("u1")) == "\"u1\"")

  test("a value class is named after the wrapper, not the payload"):
    assert(summon[PrettyPrintable[StructUserId]].printedType.simpleName == "StructUserId")

  test("a value class field renders as its unwrapped payload"):
    val actual = summon[PrettyPrintable[StructWithValueClass]].asString(StructWithValueClass(StructUserId("u1"), "bob"))
    assert(actual == "StructWithValueClass(id = \"u1\", name = \"bob\")")

  test("a value class field prints the wrapper as its declared type"):
    val conf   = Configuration(useTypeNames = true)
    val actual =
      summon[PrettyPrintable[StructWithValueClass]].asString(StructWithValueClass(StructUserId("u1"), "bob"))(using
        conf)
    assert(actual == "StructWithValueClass(id: StructUserId = \"u1\", name: String = \"bob\")")

  test("a redacted value class field is never unwrapped"):
    val actual =
      summon[PrettyPrintable[StructRedactedValueClass]].asString(StructRedactedValueClass(StructUserId(null)))
    assert(actual == "StructRedactedValueClass(id = <redacted>)")

  // A value class is rendered by the instance its companion supplies, and that instance reads the payload directly.
  // Nothing ever inspects the value class's own parameter, so an annotation written there has no effect; annotations
  // are honoured on the fields of the enclosing case class, which is where they belong.
  test("an annotation on a value class's own parameter has no effect"):
    val actual = summon[PrettyPrintable[StructTaggedHolder]].asString(StructTaggedHolder(StructTaggedId("t1")))
    assert(actual == "StructTaggedHolder(tag = \"t1\")")

  // Scala 3 synthesises no Mirror for a value class, so Magnolia never sees one and `join` is never handed a CaseClass
  // with `isValueClass = true`. This test is what licenses `join` to carry no value-class branch: if a future compiler
  // or Magnolia release starts deriving value classes, this test fails and the branch has to come back.
  test("a value class cannot be auto-derived, so Magnolia never reaches a value-class branch"):
    assertDoesNotCompile("final case class VcProbe(v: String) extends AnyVal derives PrettyPrintable")

  // Rendering is a recursive descent, so nesting depth is bounded by the stack rather than by the library. Sixty-four
  // levels is far inside that bound; the limitation itself is documented in the README rather than pinned to a number.
  test("a deeply nested recursive value renders without exhausting the stack"):
    val leaf     = StructNode("leaf", None)
    val deep     = (1 to 64).foldLeft(leaf)((inner, _) => StructNode("node", Some(inner)))
    val rendered =
      summon[PrettyPrintable[StructNode]].asString(deep)(using Configuration(multilineIfFieldsAreGreaterOrEqual = -1))
    // Each of the 64 wrappers opens `StructNode(` and `Some(`; the leaf opens only `StructNode(`.
    assert(rendered.count(_ == '(') == 64 * 2 + 1)

  // Magnolia treats a tuple as an ordinary product, so it renders structurally with its synthetic _1/_2 field names
  // and its elements go through their own instances. Pinned here because nothing else in this module covers tuples.
  test("a tuple field renders structurally, through magnolia's product derivation"):
    assert(
      summon[PrettyPrintable[StructTupleHolder]].asString(StructTupleHolder((1, "a")))(using
        Configuration(multilineIfFieldsAreGreaterOrEqual =
          -1
        )) == """StructTupleHolder(pair = Tuple2(_1 = 1, _2 = "a"))"""
    )

  test("a tuple's elements are rendered by their own instances, not by toString"):
    assert(
      summon[PrettyPrintable[StructTupleNestedHolder]].asString(StructTupleNestedHolder((StructLeaf(1), "t")))(using
        Configuration(multilineIfFieldsAreGreaterOrEqual =
          -1
        )) == """StructTupleNestedHolder(pair = Tuple2(_1 = StructLeaf(v = 1), _2 = "t"))"""
    )

final case class StructLeaf(v: Int) derives PrettyPrintable
final case class StructTupleHolder(pair: (Int, String)) derives PrettyPrintable
final case class StructTupleNestedHolder(pair: (StructLeaf, String)) derives PrettyPrintable
