package com.worxbend.reveal

import com.worxbend.reveal.annotations.Excluded

import org.scalatest.funsuite.AnyFunSuite

final case class StrEmpty() derives PrettyPrintable

final case class StrSingle(only: String) derives PrettyPrintable

final case class StrAllExcluded(@Excluded a: String, @Excluded b: String) derives PrettyPrintable

final case class StrLeaf(value: Int) derives PrettyPrintable

final case class StrBranch(leaf: StrLeaf, label: String) derives PrettyPrintable

final case class StrLevel3(value: String) derives PrettyPrintable

final case class StrLevel2(inner: StrLevel3) derives PrettyPrintable

final case class StrLevel1(inner: StrLevel2) derives PrettyPrintable

final case class StrLevel0(inner: StrLevel1) derives PrettyPrintable

final case class StrLeafList(leaves: List[StrLeaf]) derives PrettyPrintable

final case class StrLeafMap(leaves: Map[String, StrLeaf]) derives PrettyPrintable

final case class StrNode(label: String, next: Option[StrNode]) derives PrettyPrintable

final case class StrTree(label: String, children: List[StrTree]) derives PrettyPrintable

final case class StrMoney(amount: BigDecimal) extends AnyVal

final case class StrWallet(balance: StrMoney) derives PrettyPrintable

case object StrSingleton derives PrettyPrintable

final case class StrSingletonHolder(marker: StrSingleton.type) derives PrettyPrintable

sealed trait StrShape derives PrettyPrintable

object StrShape:

  final case class Circle(radius: Int) extends StrShape
  case object Dot                      extends StrShape

final case class StrShapeHolder(shape: StrShape) derives PrettyPrintable

enum StrColour derives PrettyPrintable:

  case Red
  case Mixed(hex: String)

final case class StrColourHolder(colour: StrColour) derives PrettyPrintable

/** Structural shapes: emptiness, nesting, recursion, singletons, sealed families and value classes. */
final class StructureSuite extends AnyFunSuite:

  private val singleLine: Configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = -1)

  test("an empty case class renders with empty parentheses"):
    assert(PrettyPrintable[StrEmpty].describe(StrEmpty())(using singleLine) == "StrEmpty()")

  test("a case class whose fields are all excluded renders with empty parentheses"):
    assert(PrettyPrintable[StrAllExcluded].describe(StrAllExcluded("a", "b"))(using singleLine) == "StrAllExcluded()")

  test("a single field case class renders one field"):
    assert(PrettyPrintable[StrSingle].describe(StrSingle("x"))(using singleLine) == "StrSingle(only = \"x\")")

  test("a nested case class renders through the same machinery"):
    assert(
      PrettyPrintable[StrBranch].describe(StrBranch(StrLeaf(1), "b"))(using singleLine) ==
        "StrBranch(leaf = StrLeaf(value = 1), label = \"b\")"
    )

  test("a null nested case class renders as null"):
    assert(
      PrettyPrintable[StrBranch].describe(StrBranch(null, "b"))(using
        singleLine) == "StrBranch(leaf = null, label = \"b\")"
    )

  test("four levels of nesting render on a single line"):
    assert(
      PrettyPrintable[StrLevel0].describe(StrLevel0(StrLevel1(StrLevel2(StrLevel3("deep")))))(using singleLine) ==
        "StrLevel0(inner = StrLevel1(inner = StrLevel2(inner = StrLevel3(value = \"deep\"))))"
    )

  test("a list of nested case classes renders each element"):
    assert(
      PrettyPrintable[StrLeafList].describe(StrLeafList(List(StrLeaf(1), StrLeaf(2))))(using singleLine) ==
        "StrLeafList(leaves = [StrLeaf(value = 1), StrLeaf(value = 2)])"
    )

  test("a map of nested case classes renders each value"):
    assert(
      PrettyPrintable[StrLeafMap].describe(StrLeafMap(Map("a" -> StrLeaf(1))))(using singleLine) ==
        "StrLeafMap(leaves = [\"a\" -> StrLeaf(value = 1)])"
    )

  test("a self recursive type renders through its own instance"):
    assert(
      PrettyPrintable[StrNode].describe(StrNode("a", Some(StrNode("b", None))))(using singleLine) ==
        "StrNode(label = \"a\", next = Some(StrNode(label = \"b\", next = None)))"
    )

  test("a recursive type recurses to arbitrary depth at runtime"):
    val deep = StrNode("a", Some(StrNode("b", Some(StrNode("c", Some(StrNode("d", None)))))))
    assert(
      PrettyPrintable[StrNode].describe(deep)(using singleLine) ==
        "StrNode(label = \"a\", next = Some(StrNode(label = \"b\", next = Some(StrNode(label = \"c\", " +
        "next = Some(StrNode(label = \"d\", next = None)))))))"
    )

  test("a type recursive through a collection renders"):
    assert(
      PrettyPrintable[StrTree].describe(StrTree("root", List(StrTree("child", Nil))))(using singleLine) ==
        "StrTree(label = \"root\", children = [StrTree(label = \"child\", children = [])])"
    )

  test("a value class field unwraps to its payload"):
    assert(
      PrettyPrintable[StrWallet].describe(StrWallet(StrMoney(BigDecimal("1.50"))))(using singleLine) ==
        "StrWallet(balance = 1.50)"
    )

  test("a value class field reports its own name as the declared type"):
    assert(
      PrettyPrintable[StrWallet].describe(StrWallet(StrMoney(BigDecimal("1.50"))))(using
        singleLine.copy(useTypeNames = true)) == "StrWallet(balance: StrMoney = 1.50)"
    )

  test("a case object renders as its bare name"):
    assert(PrettyPrintable[StrSingleton.type].describe(StrSingleton)(using singleLine) == "StrSingleton")

  test("a case object field renders as its bare name"):
    assert(
      PrettyPrintable[StrSingletonHolder].describe(StrSingletonHolder(StrSingleton))(using singleLine) ==
        "StrSingletonHolder(marker = StrSingleton)"
    )

  test("a sealed trait dispatches to a case class child"):
    assert(
      PrettyPrintable[StrShapeHolder].describe(StrShapeHolder(StrShape.Circle(2)))(using singleLine) ==
        "StrShapeHolder(shape = Circle(radius = 2))"
    )

  test("a sealed trait dispatches to a case object child"):
    assert(
      PrettyPrintable[StrShapeHolder].describe(StrShapeHolder(StrShape.Dot))(using singleLine) ==
        "StrShapeHolder(shape = Dot)"
    )

  test("a sealed trait renders directly at the root"):
    assert(PrettyPrintable[StrShape].describe(StrShape.Circle(2))(using singleLine) == "Circle(radius = 2)")

  test("a singleton enum case renders as its bare name"):
    assert(
      PrettyPrintable[StrColourHolder].describe(StrColourHolder(StrColour.Red))(using singleLine) ==
        "StrColourHolder(colour = Red)"
    )

  test("an enum case with fields renders structurally"):
    assert(
      PrettyPrintable[StrColourHolder].describe(StrColourHolder(StrColour.Mixed("#fff")))(using singleLine) ==
        "StrColourHolder(colour = Mixed(hex = \"#fff\"))"
    )

  test("a null sealed value renders as null"):
    assert(PrettyPrintable[StrShapeHolder].describe(StrShapeHolder(null))(using
      singleLine) == "StrShapeHolder(shape = null)")

  test("nested renders are inserted verbatim and are not re-indented"):
    val configuration = Configuration(multiline = true)
    assert(
      PrettyPrintable[StrBranch].describe(StrBranch(StrLeaf(1), "b"))(using configuration) ==
        "StrBranch(\n  leaf = StrLeaf(\n  value = 1\n),\n  label = \"b\"\n)"
    )
