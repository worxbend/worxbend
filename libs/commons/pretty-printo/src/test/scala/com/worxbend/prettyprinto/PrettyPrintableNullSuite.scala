package com.worxbend.prettyprinto

import com.worxbend.prettyprinto.annotations.Redacted

import org.scalatest.funsuite.AnyFunSuite

final case class NullString(value: String) derives PrettyPrintable

final case class NullList(values: List[String]) derives PrettyPrintable

final case class NullMap(values: Map[String, String]) derives PrettyPrintable

final case class NullArray(values: Array[String]) derives PrettyPrintable

final case class NullOption(value: Option[String]) derives PrettyPrintable

final case class NullNested(inner: NullString) derives PrettyPrintable

final case class NullRedacted(@Redacted password: String) derives PrettyPrintable

class PrettyPrintableNullSuite extends AnyFunSuite:

  test("a null string field renders as the bare null literal"):
    assert(summon[PrettyPrintable[NullString]].asString(NullString(null)) == "NullString(value = null)")

  test("a null collection field renders as null, not as empty brackets"):
    assert(summon[PrettyPrintable[NullList]].asString(NullList(null)) == "NullList(values = null)")

  test("a null Map field renders as null"):
    assert(summon[PrettyPrintable[NullMap]].asString(NullMap(null)) == "NullMap(values = null)")

  test("a null Array field renders as null"):
    assert(summon[PrettyPrintable[NullArray]].asString(NullArray(null)) == "NullArray(values = null)")

  test("a null Option field renders as null, not as None"):
    assert(summon[PrettyPrintable[NullOption]].asString(NullOption(null)) == "NullOption(value = null)")

  test("a null nested case class renders as null"):
    assert(summon[PrettyPrintable[NullNested]].asString(NullNested(null)) == "NullNested(inner = null)")

  test("a null element inside a collection renders as null"):
    assert(summon[PrettyPrintable[NullList]].asString(NullList(List("a", null))) == "NullList(values = [\"a\", null])")

  test("a null Map value renders as null"):
    assert(summon[PrettyPrintable[NullMap]].asString(NullMap(Map("k" -> null))) == "NullMap(values = [\"k\" -> null])")

  test("a null Map key renders as null"):
    assert(summon[PrettyPrintable[NullMap]].asString(NullMap(Map((null, "v")))) == "NullMap(values = [null -> \"v\"])")

  test("a null Option payload renders as Some(null)"):
    assert(summon[PrettyPrintable[NullOption]].asString(NullOption(Some(null))) == "NullOption(value = Some(null))")

  test("valuePrefix and valueSuffix still wrap a null"):
    val conf = Configuration(valuePrefix = "[", valueSuffix = "]")
    assert(summon[PrettyPrintable[NullString]].asString(NullString(null))(using conf) == "NullString(value = [null])")

  test("a null field still prints its declared type under useTypeNames"):
    val conf = Configuration(useTypeNames = true)
    assert(summon[PrettyPrintable[NullString]].asString(NullString(null))(using
      conf) == "NullString(value: String = null)")

  test("a null redacted field prints the replacement rather than throwing"):
    assert(summon[PrettyPrintable[NullRedacted]].asString(NullRedacted(null)) == "NullRedacted(password = <redacted>)")

  test("a null redacted field still prints its declared type under useTypeNames"):
    val conf   = Configuration(useTypeNames = true)
    val actual = summon[PrettyPrintable[NullRedacted]].asString(NullRedacted(null))(using conf)
    assert(actual == "NullRedacted(password: String = <redacted>)")
