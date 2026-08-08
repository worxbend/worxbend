package com.worxbend.describo

import org.scalatest.funsuite.AnyFunSuite

final case class LcId(raw: Long) extends AnyVal

object LcId:

  given Printable[LcId] = Printable.valueClass[LcId, Long]("LcId", "com.worxbend.describo.LcId")(_.raw)

enum LcColour derives Printable:

  case Red

final case class LcColl(xs: List[String], m: Map[String, Int]) derives Printable

final case class LcTyped(t: (Int, String), e: LcColour, id: LcId, o: Option[Int]) derives Printable

final case class LcWide(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int) derives Printable

/** The configuration options applied to composite values rather than to flat ones.
  *
  * `PrintableConfigurationSuite` exercises each option against a simple two-field record, which is the right way to
  * pin what the option means. These pin what it does once the value has structure — where an option can plausibly
  * apply at the wrong level, such as wrapping every element instead of the whole collection.
  */
final class PrintableLayoutCompositionSuite extends AnyFunSuite:

  private def render[A](value: A, configuration: Configuration)(using printable: Printable[A]): String =
    printable.asString(value)(using configuration)

  test("the multiline layout breaks fields, not the elements inside them"):
    assert(
      render(LcColl(List("a", "b"), Map("k" -> 1)), Configuration(multiline = true)) ==
        "LcColl(\n  xs = [\"a\", \"b\"],\n  m = [\"k\" -> 1]\n)"
    )

  test("empty collections still occupy their own line under multiline"):
    assert(render(LcColl(Nil, Map.empty), Configuration(multiline = true)) == "LcColl(\n  xs = [],\n  m = []\n)")

  test("the default threshold takes a six field record multiline without being asked"):
    assert(
      render(LcWide(1, 2, 3, 4, 5, 6), Configuration()) ==
        "LcWide(\n  a = 1,\n  b = 2,\n  c = 3,\n  d = 4,\n  e = 5,\n  f = 6\n)"
    )

  test("useTypeNames names the declared type at every level, including inside a tuple"):
    assert(
      render(
        LcTyped((1, "a"), LcColour.Red, LcId(1L), Some(2)),
        Configuration(useTypeNames = true, multilineIfFieldsAreGreaterOrEqual = -1),
      ) == """LcTyped(t: Tuple2 = Tuple2(_1: Int = 1, _2: String = "a"), e: LcColour = Red, id: LcId = 1, o: Option = Some(2))"""
    )

  // A value class reports the *wrapper* as its declared type while rendering the payload, and an enum case reports
  // the enum. Both are easy to get backwards once a type name and a value are produced by different code paths.
  test("a value class shows the wrapper as its type and the payload as its value"):
    assert(
      render(
        LcTyped((1, "a"), LcColour.Red, LcId(9L), None),
        Configuration(useTypeNames = true, multilineIfFieldsAreGreaterOrEqual = -1),
      ).contains("id: LcId = 9")
    )

  // The enum case renders as `c.w.d.Red` rather than `c.w.d.LcColour.Red`: Magnolia's TypeInfo reports the enclosing
  // package, not the enclosing enum. Documented in the README under Known limitations, and pinned here so the
  // qualified spelling cannot drift unnoticed.
  test("qualified names reach the field types and the enum case alike"):
    assert(
      render(
        LcTyped((1, "a"), LcColour.Red, LcId(1L), None),
        Configuration(
          useTypeNames = true,
          fullyQualifiedClassName = true,
          shortPackagePrefix = true,
          multilineIfFieldsAreGreaterOrEqual = -1,
        ),
      ) == "c.w.d.LcTyped(t: s.Tuple2 = s.Tuple2(_1: s.Int = 1, _2: j.l.String = \"a\"), " +
        "e: c.w.d.LcColour = c.w.d.Red, id: c.w.d.LcId = 1, o: s.Option = None)"
    )

  // valuePrefix/valueSuffix wrap the field's whole rendered value. A collection therefore gets one pair of affixes
  // around the entire bracketed list, not a pair per element.
  test("value affixes wrap the whole collection, not each element"):
    assert(
      render(
        LcColl(List("a"), Map("k" -> 1)),
        Configuration(valuePrefix = "[", valueSuffix = "]", multilineIfFieldsAreGreaterOrEqual = -1),
      ) == """LcColl(xs = [["a"]], m = [["k" -> 1]])"""
    )
