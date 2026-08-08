package com.worxbend.reveal

import java.time.Instant
import java.time.LocalDate

import org.scalatest.funsuite.AnyFunSuite

final case class ValText(text: String) derives PrettyPrintable

final case class ValLetter(letter: Char) derives PrettyPrintable

final case class ValNumbers(
    integer: Int,
    long:    Long,
    short:   Short,
    byte:    Byte,
    double:  Double,
    float:   Float,
    flag:    Boolean,
) derives PrettyPrintable

final case class ValBigDecimal(amount: BigDecimal) derives PrettyPrintable

final case class ValBigInt(amount: BigInt) derives PrettyPrintable

final case class ValDates(day: LocalDate, at: Instant) derives PrettyPrintable

final case class ValList(items: List[String]) derives PrettyPrintable

final case class ValVector(items: Vector[String]) derives PrettyPrintable

final case class ValSet(items: Set[String]) derives PrettyPrintable

final case class ValSeq(items: Seq[String]) derives PrettyPrintable

final case class ValIndexedSeq(items: IndexedSeq[String]) derives PrettyPrintable

final case class ValIterable(items: Iterable[String]) derives PrettyPrintable

final case class ValArray(items: Array[String]) derives PrettyPrintable

final case class ValNestedList(items: List[List[String]]) derives PrettyPrintable

final case class ValMap(entries: Map[String, String]) derives PrettyPrintable

final case class ValIntMap(entries: Map[Int, Int]) derives PrettyPrintable

final case class ValOptionText(entry: Option[String]) derives PrettyPrintable

final case class ValOptionList(entry: Option[List[String]]) derives PrettyPrintable

final case class ValJavaList(items: java.util.List[String]) derives PrettyPrintable

final case class ValJavaArrayList(items: java.util.ArrayList[String]) derives PrettyPrintable

final case class ValJavaLinkedList(items: java.util.LinkedList[String]) derives PrettyPrintable

final case class ValJavaSet(items: java.util.Set[String]) derives PrettyPrintable

final case class ValJavaHashSet(items: java.util.HashSet[String]) derives PrettyPrintable

final case class ValJavaMap(entries: java.util.Map[String, String]) derives PrettyPrintable

final case class ValJavaHashMap(entries: java.util.HashMap[String, String]) derives PrettyPrintable

/** Value-position rendering: quoting, escaping, containers and nulls. */
final class ValueRenderingSuite extends AnyFunSuite:

  private val singleLine: Configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = -1)

  private def text(value: String): String = PrettyPrintable[ValText].describe(ValText(value))(using singleLine)

  test("a string is quoted"):
    assert(text("plain") == "ValText(text = \"plain\")")

  test("a string containing a comma stays unambiguous"):
    assert(text("a,b") == "ValText(text = \"a,b\")")

  test("a string containing parentheses stays unambiguous"):
    assert(text("a)b(") == "ValText(text = \"a)b(\")")

  test("a double quote inside a string is escaped"):
    assert(text("a\"b") == "ValText(text = \"a\\\"b\")")

  test("a backslash inside a string is escaped first"):
    assert(text("a\\b") == "ValText(text = \"a\\\\b\")")

  test("a newline inside a string is escaped"):
    assert(text("a\nb") == "ValText(text = \"a\\nb\")")

  test("a carriage return inside a string is escaped"):
    assert(text("a\rb") == "ValText(text = \"a\\rb\")")

  test("a tab inside a string is escaped"):
    assert(text("a\tb") == "ValText(text = \"a\\tb\")")

  test("an empty string renders as empty quotes"):
    assert(text("") == "ValText(text = \"\")")

  test("a null string renders as null"):
    assert(text(null) == "ValText(text = null)")

  test("a char is single quoted"):
    assert(PrettyPrintable[ValLetter].describe(ValLetter('c'))(using singleLine) == "ValLetter(letter = 'c')")

  test("a single quote inside a char is escaped"):
    assert(PrettyPrintable[ValLetter].describe(ValLetter('\''))(using singleLine) == "ValLetter(letter = '\\'')")

  test("a newline char is escaped"):
    assert(PrettyPrintable[ValLetter].describe(ValLetter('\n'))(using singleLine) == "ValLetter(letter = '\\n')")

  test("numeric and boolean primitives render bare"):
    assert(
      PrettyPrintable[ValNumbers].describe(ValNumbers(1, 2L, 3.toShort, 4.toByte, 5.5d, 6.5f, true))(using
        singleLine) ==
        "ValNumbers(integer = 1, long = 2, short = 3, byte = 4, double = 5.5, float = 6.5, flag = true)"
    )

  test("a BigDecimal keeps its scale"):
    assert(
      PrettyPrintable[ValBigDecimal].describe(ValBigDecimal(BigDecimal("1000.50")))(using singleLine) ==
        "ValBigDecimal(amount = 1000.50)"
    )

  test("a BigInt renders bare"):
    assert(PrettyPrintable[ValBigInt].describe(ValBigInt(BigInt("42")))(using singleLine) == "ValBigInt(amount = 42)")

  test("java.time values render through their own toString"):
    assert(
      PrettyPrintable[ValDates]
        .describe(ValDates(LocalDate.parse("2023-01-01"), Instant.parse("2023-01-01T00:00:00Z")))(using singleLine) ==
        "ValDates(day = 2023-01-01, at = 2023-01-01T00:00:00Z)"
    )

  test("a List renders in brackets with quoted elements"):
    assert(PrettyPrintable[ValList].describe(ValList(List("a", "b")))(using
      singleLine) == "ValList(items = [\"a\", \"b\"])")

  test("an empty List renders as empty brackets"):
    assert(PrettyPrintable[ValList].describe(ValList(Nil))(using singleLine) == "ValList(items = [])")

  test("a null List renders as null, not as empty brackets"):
    assert(PrettyPrintable[ValList].describe(ValList(null))(using singleLine) == "ValList(items = null)")

  test("a Vector renders in brackets"):
    assert(
      PrettyPrintable[ValVector].describe(ValVector(Vector("a", "b")))(using
        singleLine) == "ValVector(items = [\"a\", \"b\"])"
    )

  test("a Set renders in brackets"):
    assert(PrettyPrintable[ValSet].describe(ValSet(Set("a", "b")))(using
      singleLine) == "ValSet(items = [\"a\", \"b\"])")

  test("a Seq renders in brackets"):
    assert(PrettyPrintable[ValSeq].describe(ValSeq(Seq("a", "b")))(using
      singleLine) == "ValSeq(items = [\"a\", \"b\"])")

  test("an IndexedSeq renders in brackets"):
    assert(
      PrettyPrintable[ValIndexedSeq].describe(ValIndexedSeq(IndexedSeq("a", "b")))(using singleLine) ==
        "ValIndexedSeq(items = [\"a\", \"b\"])"
    )

  test("an Iterable renders in brackets"):
    assert(
      PrettyPrintable[ValIterable].describe(ValIterable(Iterable("a", "b")))(using singleLine) ==
        "ValIterable(items = [\"a\", \"b\"])"
    )

  test("an Array renders in brackets"):
    assert(
      PrettyPrintable[ValArray].describe(ValArray(Array("a", "b")))(using
        singleLine) == "ValArray(items = [\"a\", \"b\"])"
    )

  test("a null Array renders as null"):
    assert(PrettyPrintable[ValArray].describe(ValArray(null))(using singleLine) == "ValArray(items = null)")

  test("nested collections render recursively"):
    assert(
      PrettyPrintable[ValNestedList].describe(ValNestedList(List(List("a"), List("b", "c"))))(using singleLine) ==
        "ValNestedList(items = [[\"a\"], [\"b\", \"c\"]])"
    )

  test("a Map renders as bracketed arrow entries"):
    assert(
      PrettyPrintable[ValMap].describe(ValMap(Map("key1" -> "value1", "key2" -> "value2")))(using singleLine) ==
        "ValMap(entries = [\"key1\" -> \"value1\", \"key2\" -> \"value2\"])"
    )

  test("a Map with non string keys renders both sides by the value rules"):
    assert(
      PrettyPrintable[ValIntMap].describe(ValIntMap(Map(1 -> 2)))(using singleLine) == "ValIntMap(entries = [1 -> 2])"
    )

  test("an empty Map renders as empty brackets"):
    assert(PrettyPrintable[ValMap].describe(ValMap(Map.empty))(using singleLine) == "ValMap(entries = [])")

  test("a null Map renders as null"):
    assert(PrettyPrintable[ValMap].describe(ValMap(null))(using singleLine) == "ValMap(entries = null)")

  test("Some quotes its payload"):
    assert(
      PrettyPrintable[ValOptionText].describe(ValOptionText(Some("hi")))(using singleLine) ==
        "ValOptionText(entry = Some(\"hi\"))"
    )

  test("None renders as None"):
    assert(PrettyPrintable[ValOptionText].describe(ValOptionText(None))(using
      singleLine) == "ValOptionText(entry = None)")

  test("a null Option renders as null, not as None"):
    assert(PrettyPrintable[ValOptionText].describe(ValOptionText(null))(using
      singleLine) == "ValOptionText(entry = null)")

  test("a null Option payload renders as null"):
    assert(
      PrettyPrintable[ValOptionText].describe(ValOptionText(Some(null)))(using singleLine) ==
        "ValOptionText(entry = Some(null))"
    )

  test("an Option of a collection renders both layers"):
    assert(
      PrettyPrintable[ValOptionList].describe(ValOptionList(Some(List("a"))))(using singleLine) ==
        "ValOptionList(entry = Some([\"a\"]))"
    )

  test("a java.util.List renders in brackets"):
    assert(
      PrettyPrintable[ValJavaList].describe(ValJavaList(java.util.List.of("a", "b")))(using singleLine) ==
        "ValJavaList(items = [\"a\", \"b\"])"
    )

  test("a java.util.ArrayList renders in brackets"):
    assert(
      PrettyPrintable[ValJavaArrayList]
        .describe(ValJavaArrayList(java.util.ArrayList(java.util.List.of("a", "b"))))(using singleLine) ==
        "ValJavaArrayList(items = [\"a\", \"b\"])"
    )

  test("a java.util.LinkedList renders in brackets"):
    assert(
      PrettyPrintable[ValJavaLinkedList]
        .describe(ValJavaLinkedList(java.util.LinkedList(java.util.List.of("a"))))(using singleLine) ==
        "ValJavaLinkedList(items = [\"a\"])"
    )

  test("a java.util.Set renders in brackets"):
    assert(
      PrettyPrintable[ValJavaSet].describe(ValJavaSet(java.util.Set.of("a")))(using singleLine) ==
        "ValJavaSet(items = [\"a\"])"
    )

  test("a java.util.HashSet renders in brackets"):
    assert(
      PrettyPrintable[ValJavaHashSet]
        .describe(ValJavaHashSet(java.util.HashSet(java.util.List.of("a"))))(using singleLine) ==
        "ValJavaHashSet(items = [\"a\"])"
    )

  test("a null java collection renders as null"):
    assert(PrettyPrintable[ValJavaList].describe(ValJavaList(null))(using singleLine) == "ValJavaList(items = null)")

  test("a java.util.Map renders in the same bracket form as a Scala Map"):
    assert(
      PrettyPrintable[ValJavaMap].describe(ValJavaMap(java.util.Map.of("k", "v")))(using singleLine) ==
        "ValJavaMap(entries = [\"k\" -> \"v\"])"
    )

  test("a java.util.HashMap renders in brackets, not in braces"):
    assert(
      PrettyPrintable[ValJavaHashMap]
        .describe(ValJavaHashMap(java.util.HashMap(java.util.Map.of("k", "v"))))(using singleLine) ==
        "ValJavaHashMap(entries = [\"k\" -> \"v\"])"
    )
