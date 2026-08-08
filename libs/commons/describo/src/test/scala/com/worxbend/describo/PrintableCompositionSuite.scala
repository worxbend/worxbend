package com.worxbend.describo

import com.worxbend.describo.annotations.Excluded
import com.worxbend.describo.annotations.Redacted

import org.scalatest.funsuite.AnyFunSuite

final case class CmpLeaf(v: Int) derives Printable

final case class CmpSecret(@Redacted token: String, tag: String) derives Printable

final case class CmpId(raw: Long) extends AnyVal

object CmpId:

  given Printable[CmpId] = Printable.valueClass[CmpId, Long]("CmpId", "com.worxbend.describo.CmpId")(_.raw)

enum CmpColour derives Printable:

  case Red
  case Sized(n: Int, label: String)

enum CmpSecretive derives Printable:

  case Token(@Redacted value: String, kind: String)

sealed trait CmpShape derives Printable
final case class CmpCircle(r: Int) extends CmpShape
case object CmpDot                 extends CmpShape

/** How the individual rendering rules behave once they are combined.
  *
  * The existing suites each cover one axis — collections, structure, values, configuration. Everything below is a
  * *combination* of two or more of them, which is where a rule that reads correctly in isolation tends to go wrong:
  * an element renderer that forgets to recurse, a value class that survives one level of wrapping but not two, an
  * annotation that stops composing once a collection is involved.
  */
final class PrintableCompositionSuite extends AnyFunSuite:

  private val flat: Configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = -1)

  private def render[A](value: A)(using printable: Printable[A]): String = printable.asString(value)(using flat)

  // ------------------------------------------------------------- tuples

  test("a three element tuple renders all three positions"):
    final case class Holder(t: (Int, String, Boolean)) derives Printable
    assert(render(Holder((1, "a", true))) == """Holder(t = Tuple3(_1 = 1, _2 = "a", _3 = true))""")

  test("a tuple inside a list is rendered per element"):
    final case class Holder(xs: List[(Int, String)]) derives Printable
    assert(render(Holder(List((1, "a")))) == """Holder(xs = [Tuple2(_1 = 1, _2 = "a")])""")

  test("a tuple nested in a tuple recurses"):
    final case class Holder(t: ((Int, String), Boolean)) derives Printable
    assert(render(Holder(((1, "a"), true))) == """Holder(t = Tuple2(_1 = Tuple2(_1 = 1, _2 = "a"), _2 = true))""")

  test("a tuple carries a value class and an enum through their own instances"):
    final case class Holder(t: (CmpId, CmpColour)) derives Printable
    assert(render(Holder((CmpId(1L), CmpColour.Red))) == "Holder(t = Tuple2(_1 = 1, _2 = Red))")

  test("a null inside a tuple renders as null rather than throwing"):
    final case class Holder(t: (String, String)) derives Printable
    assert(render(Holder((null, "a"))) == """Holder(t = Tuple2(_1 = null, _2 = "a"))""")

  // -------------------------------------------------- collections of things

  test("a list of enum values renders singleton and parameterised cases side by side"):
    final case class Holder(xs: List[CmpColour]) derives Printable
    assert(render(Holder(List(CmpColour.Red, CmpColour.Sized(2, "m")))) ==
      """Holder(xs = [Red, Sized(n = 2, label = "m")])""")

  test("a list of value classes unwraps each element"):
    final case class Holder(xs: List[CmpId]) derives Printable
    assert(render(Holder(List(CmpId(7L), CmpId(8L)))) == "Holder(xs = [7, 8])")

  test("a list of a sealed trait dispatches per element"):
    final case class Holder(xs: List[CmpShape]) derives Printable
    assert(render(Holder(List(CmpCircle(1), CmpDot))) == "Holder(xs = [CmpCircle(r = 1), CmpDot])")

  test("an Array of case classes renders structurally, not as a JVM identity"):
    final case class Holder(xs: Array[CmpLeaf]) derives Printable
    assert(render(Holder(Array(CmpLeaf(1)))) == "Holder(xs = [CmpLeaf(v = 1)])")

  test("escaping applies to elements, not only to top level fields"):
    final case class Holder(xs: List[String]) derives Printable
    assert(render(Holder(List("a,b", "c\"d"))) == """Holder(xs = ["a,b", "c\"d"])""")

  // ---------------------------------------------------------------- maps

  test("a value class works as a map key"):
    final case class Holder(m: Map[CmpId, CmpLeaf]) derives Printable
    assert(render(Holder(Map(CmpId(1L) -> CmpLeaf(2)))) == "Holder(m = [1 -> CmpLeaf(v = 2)])")

  test("a map whose values are lists renders both layers"):
    final case class Holder(m: Map[String, List[CmpLeaf]]) derives Printable
    assert(render(Holder(Map("k" -> List(CmpLeaf(1))))) == """Holder(m = ["k" -> [CmpLeaf(v = 1)]])""")

  // ------------------------------------------------------------- options

  test("an Option of an enum keeps the enum's own rendering"):
    final case class Holder(o: Option[CmpColour]) derives Printable
    assert(render(Holder(Some(CmpColour.Red))) == "Holder(o = Some(Red))")

  test("a nested Option renders both layers"):
    final case class Holder(oo: Option[Option[String]]) derives Printable
    assert(render(Holder(Some(Some("x")))) == """Holder(oo = Some(Some("x")))""")

  test("an Option of a value class unwraps the payload inside the Some"):
    final case class Holder(o: Option[CmpId]) derives Printable
    assert(render(Holder(Some(CmpId(3L)))) == "Holder(o = Some(3))")

  // ------------------------------------------------------- sealed generics

  test("Either dispatches to Right and renders the payload through its instance"):
    final case class Holder(e: Either[String, CmpLeaf]) derives Printable
    assert(render(Holder(Right(CmpLeaf(1)))) == "Holder(e = Right(value = CmpLeaf(v = 1)))")

  test("Either dispatches to Left"):
    final case class Holder(e: Either[String, CmpLeaf]) derives Printable
    assert(render(Holder(Left("boom"))) == """Holder(e = Left(value = "boom"))""")

  // The type set is closed, so a shape is only renderable when every type it reaches has an instance. `Try` reaches
  // `Throwable` through `Failure`, and this module ships no instance for it — unlike `reveal`, whose macro renders
  // any concrete type by `toString`. Pinned so the difference is a decision rather than a surprise.
  test("Try is not renderable, because Failure reaches Throwable and nothing supplies an instance"):
    assertDoesNotCompile("summon[Printable[scala.util.Try[Int]]]")

  test("supplying the missing instance is all Try needs"):
    given Printable[Throwable] = Printable.instance("Throwable", "java.lang.Throwable")(_.getMessage)
    assert(
      summon[Printable[scala.util.Try[Int]]].asString(scala.util.Success(1))(using flat) == "Success(value = 1)"
    )

  // -------------------------------------------------------- redaction holds

  test("redaction survives being inside a list"):
    final case class Holder(xs: List[CmpSecret]) derives Printable
    assert(render(Holder(List(CmpSecret("hunter2", "t")))) ==
      """Holder(xs = [CmpSecret(token = <redacted>, tag = "t")])""")

  test("redaction survives being a map value"):
    final case class Holder(m: Map[String, CmpSecret]) derives Printable
    assert(render(Holder(Map("k" -> CmpSecret("hunter2", "t")))) ==
      """Holder(m = ["k" -> CmpSecret(token = <redacted>, tag = "t")])""")

  test("redaction survives being inside a tuple"):
    final case class Holder(t: (Int, CmpSecret)) derives Printable
    assert(render(Holder((1, CmpSecret("hunter2", "t")))) ==
      """Holder(t = Tuple2(_1 = 1, _2 = CmpSecret(token = <redacted>, tag = "t")))""")

  test("redaction survives inside an enum case"):
    final case class Holder(e: CmpSecretive) derives Printable
    assert(render(Holder(CmpSecretive.Token("hunter2", "api"))) ==
      """Holder(e = Token(value = <redacted>, kind = "api"))""")

  test("an excluded field is dropped from an element inside a collection"):
    final case class Element(@Excluded hidden: String, shown: Int) derives Printable
    final case class Holder(xs: List[Element]) derives Printable
    assert(render(Holder(List(Element("secret", 1)))) == "Holder(xs = [Element(shown = 1)])")
