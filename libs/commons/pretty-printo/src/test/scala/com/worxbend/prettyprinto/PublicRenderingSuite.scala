package com.worxbend.prettyprinto

import org.scalatest.funsuite.AnyFunSuite

object PublicRenderingFixtures:

  final case class Product(text: String) derives PrettyPrintable
  sealed trait Event derives PrettyPrintable
  case object Stop extends Event

  enum Status derives PrettyPrintable:

    case Ready
    case Failed(reason: String)

  final class Wrapped(val text: String)

final class PublicRenderingSuite extends AnyFunSuite:

  import PublicRenderingFixtures.*

  private def assertNull[T](using printable: PrettyPrintable[T]): Unit =
    assert(printable.asString(null.asInstanceOf[T]) == "null")

  test("direct scalar and boxed Java nulls render null"):
    assertNull[String]
    assertNull[java.lang.Integer]
    assertNull[java.time.Instant]

  test("direct product null renders null before reading fields"):
    assertNull[Product]

  test("direct sealed and enum nulls render null before selecting a branch"):
    assertNull[Event]
    assertNull[Status]

  test("direct singleton null renders null instead of the singleton name"):
    assertNull[Stop.type]

  test("direct container nulls remain distinct from empty containers"):
    assertNull[List[String]]
    assertNull[Array[String]]
    assertNull[Map[String, String]]
    assertNull[java.util.List[String]]
    assertNull[java.util.Map[String, String]]
    assert(summon[PrettyPrintable[List[String]]].asString(Nil) == "[]")

  test("direct Option null remains distinct from None and Some(null)"):
    val printable = summon[PrettyPrintable[Option[String]]]
    assert(printable.asString(null) == "null")
    assert(printable.asString(None) == "None")
    assert(printable.asString(Some(null)) == "Some(null)")

  test("instance factory skips the callback for null and calls it once otherwise"):
    var calls     = 0
    val printable = PrettyPrintable.instance[Wrapped]("Wrapped", "example.Wrapped"): wrapped =>
      calls += 1
      wrapped.text
    assert(printable.asString(null) == "null")
    assert(calls == 0)
    assert(printable.asString(Wrapped("ok")) == "ok")
    assert(calls == 1)

  test("collection factory never requests an iterator from null"):
    val printable =
      PrettyPrintable.collection[Wrapped, String]("Wrapped", "example.Wrapped")(_.text.iterator.map(_.toString))
    assert(printable.asString(null) == "null")
    assert(printable.asString(Wrapped("a")) == "[\"a\"]")

  test("mapping factory never requests entries from null"):
    val printable = PrettyPrintable.mapping[Wrapped, String, String]("Wrapped", "example.Wrapped")(wrapped =>
      Iterator.single(("key", wrapped.text))
    )
    assert(printable.asString(null) == "null")
    assert(printable.asString(Wrapped("ok")) == "[\"key\" -> \"ok\"]")

  test("valueClass factory never unwraps null"):
    val printable = PrettyPrintable.valueClass[Wrapped, String]("Wrapped", "example.Wrapped")(_.text)
    assert(printable.asString(null) == "null")
    assert(printable.asString(Wrapped("ok")) == "\"ok\"")

  test("factory callback exceptions propagate unchanged"):
    val failure   = IllegalStateException("renderer failed")
    val printable = PrettyPrintable.instance[Wrapped]("Wrapped", "example.Wrapped")(_ => throw failure)
    assert(intercept[IllegalStateException](printable.asString(Wrapped("ok"))) eq failure)

  test("direct null ignores field formatting configuration"):
    given Configuration = Configuration(useTypeNames = true, valuePrefix = "<", valueSuffix = ">", multiline = true)
    assert(summon[PrettyPrintable[Product]].asString(null) == "null")
