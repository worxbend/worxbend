package com.worxbend.describo

import com.worxbend.describo.annotations.Excluded
import com.worxbend.describo.annotations.Redacted

import org.scalatest.funsuite.AnyFunSuite

final case class AnnDefaultRedaction(@Redacted password: String, name: String) derives Printable

final case class AnnCustomRedaction(@Redacted(replacement = "***") password: String, name: String) derives Printable

final case class AnnExcluded(@Excluded secret: String, name: String) derives Printable

final case class AnnTransient(@transient secret: String, name: String) derives Printable

final case class AnnRedactedThenExcluded(@Redacted @Excluded both: String, name: String) derives Printable

final case class AnnExcludedThenRedacted(@Excluded @Redacted both: String, name: String) derives Printable

final case class AnnMultipleRedactions(
    @Redacted(replacement = "<written-first>") @Redacted(replacement = "<written-last>") password: String,
    name:                                                                                          String,
) derives Printable

final case class AnnRedactedInt(@Redacted count: Int, name: String) derives Printable

object AnnNesting:

  final case class Inner(@Redacted secret: String, label: String) derives Printable
  final case class Outer(inner: Inner, name: String) derives Printable

final case class AnnFiveWithTwoExcluded(
    @Excluded a: Int,
    @Excluded b: Int,
    c:           Int,
    d:           Int,
    e:           Int,
) derives Printable

class PrintableAnnotationsSuite extends AnyFunSuite:

  test("@Redacted prints the default replacement"):
    val actual = summon[Printable[AnnDefaultRedaction]].asString(AnnDefaultRedaction("hunter2", "bob"))
    assert(actual == "AnnDefaultRedaction(password = <redacted>, name = \"bob\")")

  test("@Redacted prints a custom replacement"):
    val actual = summon[Printable[AnnCustomRedaction]].asString(AnnCustomRedaction("hunter2", "bob"))
    assert(actual == "AnnCustomRedaction(password = ***, name = \"bob\")")

  test("@Redacted still prints the declared type under useTypeNames"):
    val actual =
      summon[Printable[AnnDefaultRedaction]].asString(AnnDefaultRedaction("hunter2", "bob"))(using
        Configuration(useTypeNames = true))
    assert(actual == "AnnDefaultRedaction(password: String = <redacted>, name: String = \"bob\")")

  test("@Redacted never dereferences the value, so a null secret prints the replacement"):
    val actual = summon[Printable[AnnDefaultRedaction]].asString(AnnDefaultRedaction(null, "bob"))
    assert(actual == "AnnDefaultRedaction(password = <redacted>, name = \"bob\")")

  test("@Redacted on a non-string field prints the replacement rather than the number"):
    assert(summon[Printable[AnnRedactedInt]].asString(AnnRedactedInt(42, "bob")).contains("count = <redacted>"))

  test("@Excluded omits the field entirely"):
    assert(summon[Printable[AnnExcluded]].asString(AnnExcluded("s", "bob")) == "AnnExcluded(name = \"bob\")")

  test("@Excluded never dereferences the value, so a null excluded field does not throw"):
    assert(summon[Printable[AnnExcluded]].asString(AnnExcluded(null, "bob")) == "AnnExcluded(name = \"bob\")")

  test("@transient omits the field, exactly like @Excluded"):
    assert(summon[Printable[AnnTransient]].asString(AnnTransient("s", "bob")) == "AnnTransient(name = \"bob\")")

  test("exclusion beats redaction when @Redacted is written first"):
    val actual = summon[Printable[AnnRedactedThenExcluded]].asString(AnnRedactedThenExcluded("s", "bob"))
    assert(actual == "AnnRedactedThenExcluded(name = \"bob\")")

  test("exclusion beats redaction when @Excluded is written first"):
    val actual = summon[Printable[AnnExcludedThenRedacted]].asString(AnnExcludedThenRedacted("s", "bob"))
    assert(actual == "AnnExcludedThenRedacted(name = \"bob\")")

  // The rule is "the @Redacted written first in source order wins", which is what reveal's macro implements.
  // Magnolia surfaces `param.annotations` in reverse source order, so FieldRule.of reverses before searching;
  // this test pins the agreed behaviour so the two modules cannot drift apart again.
  test("several @Redacted annotations on one field resolve to the one written first"):
    val actual = summon[Printable[AnnMultipleRedactions]].asString(AnnMultipleRedactions("s", "bob"))
    assert(actual == "AnnMultipleRedactions(password = <written-first>, name = \"bob\")")

  test("redaction composes through a nested case class"):
    val value  = AnnNesting.Outer(AnnNesting.Inner("s", "l"), "x")
    val actual = summon[Printable[AnnNesting.Outer]].asString(value)
    assert(actual == "Outer(inner = Inner(secret = <redacted>, label = \"l\"), name = \"x\")")

  test("the multiline threshold counts fields after exclusion"):
    val value  = AnnFiveWithTwoExcluded(1, 2, 3, 4, 5)
    val actual = summon[Printable[AnnFiveWithTwoExcluded]].asString(value)
    assert(actual == "AnnFiveWithTwoExcluded(c = 3, d = 4, e = 5)")
