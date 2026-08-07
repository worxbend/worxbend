package com.worxbend.describo

import com.worxbend.describo.annotations.Redacted

import org.scalatest.funsuite.AnyFunSuite

final case class NullString(value: String) derives Printable

final case class NullList(values: List[String]) derives Printable

final case class NullMap(values: Map[String, String]) derives Printable

final case class NullArray(values: Array[String]) derives Printable

final case class NullOption(value: Option[String]) derives Printable

final case class NullNested(inner: NullString) derives Printable

final case class NullRedacted(@Redacted password: String) derives Printable

class PrintableNullSuite extends AnyFunSuite:

  test("a null string field renders as the bare null literal"):
    assert(summon[Printable[NullString]].asString(NullString(null)) == "NullString(value = null)")

  test("a null collection field renders as null, not as empty brackets"):
    assert(summon[Printable[NullList]].asString(NullList(null)) == "NullList(values = null)")

  test("a null Map field renders as null"):
    assert(summon[Printable[NullMap]].asString(NullMap(null)) == "NullMap(values = null)")

  test("a null Array field renders as null"):
    assert(summon[Printable[NullArray]].asString(NullArray(null)) == "NullArray(values = null)")

  test("a null Option field renders as null, not as None"):
    assert(summon[Printable[NullOption]].asString(NullOption(null)) == "NullOption(value = null)")

  test("a null nested case class renders as null"):
    assert(summon[Printable[NullNested]].asString(NullNested(null)) == "NullNested(inner = null)")

  test("a null element inside a collection renders as null"):
    assert(summon[Printable[NullList]].asString(NullList(List("a", null))) == "NullList(values = [\"a\", null])")

  test("a null Map value renders as null"):
    assert(summon[Printable[NullMap]].asString(NullMap(Map("k" -> null))) == "NullMap(values = [\"k\" -> null])")

  test("a null Map key renders as null"):
    assert(summon[Printable[NullMap]].asString(NullMap(Map((null, "v")))) == "NullMap(values = [null -> \"v\"])")

  test("a null Option payload renders as Some(null)"):
    assert(summon[Printable[NullOption]].asString(NullOption(Some(null))) == "NullOption(value = Some(null))")

  test("valuePrefix and valueSuffix still wrap a null"):
    val conf = Configuration(valuePrefix = "[", valueSuffix = "]")
    assert(summon[Printable[NullString]].asString(NullString(null))(using conf) == "NullString(value = [null])")

  test("a null field still prints its declared type under useTypeNames"):
    val conf = Configuration(useTypeNames = true)
    assert(summon[Printable[NullString]].asString(NullString(null))(using conf) == "NullString(value: String = null)")

  test("a null redacted field prints the replacement rather than throwing"):
    assert(summon[Printable[NullRedacted]].asString(NullRedacted(null)) == "NullRedacted(password = <redacted>)")

  test("a null redacted field still prints its declared type under useTypeNames"):
    val conf   = Configuration(useTypeNames = true)
    val actual = summon[Printable[NullRedacted]].asString(NullRedacted(null))(using conf)
    assert(actual == "NullRedacted(password: String = <redacted>)")
