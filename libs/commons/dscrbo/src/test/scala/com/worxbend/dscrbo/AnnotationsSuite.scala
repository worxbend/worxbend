package com.worxbend.dscrbo

import com.worxbend.dscrbo.annotations.Excluded
import com.worxbend.dscrbo.annotations.Redacted

import org.scalatest.funsuite.AnyFunSuite

final case class AnnDefaultRedaction(@Redacted password: String, name: String) derives Describe

final case class AnnCustomRedaction(@Redacted(replacement = "***") password: String, name: String) derives Describe

final case class AnnNamedDefaultRedaction(@Redacted(replacement = "<redacted>") password: String) derives Describe

final case class AnnExcluded(@Excluded hidden: String, name: String) derives Describe

final case class AnnTransient(@transient hidden: String, name: String) derives Describe

final case class AnnRedactedThenExcluded(@Redacted @Excluded both: String, name: String) derives Describe

final case class AnnExcludedThenRedacted(@Excluded @Redacted both: String, name: String) derives Describe

final case class AnnRepeatedRedaction(@Redacted(replacement = "first") @Redacted(replacement = "second") token: String)
    derives Describe

final case class AnnNestedSecret(@Redacted secret: String, tag: String) derives Describe

final case class AnnHolder(inner: AnnNestedSecret, name: String) derives Describe

final case class AnnListHolder(entries: List[AnnNestedSecret]) derives Describe

final case class AnnOptionHolder(entry: Option[AnnNestedSecret]) derives Describe

final case class AnnMapHolder(entries: Map[String, AnnNestedSecret]) derives Describe

/** Recursive and deliberately without an instance: it can only be rendered as a root, never inlined into a parent. */
final case class AnnUnrenderable(children: List[AnnUnrenderable])

final case class AnnUnrenderableHolder(child: AnnUnrenderable)

/** Compiles only because the annotated fields are never rendered, so their type is never inspected. */
final case class AnnNeverDereferenced(
    @Redacted secret: AnnUnrenderable,
    @Excluded hidden: AnnUnrenderable,
    name:             String,
) derives Describe

final case class AnnRedactedValueClass(@Redacted amount: BigDecimal) extends AnyVal

final case class AnnExcludedValueClass(@Excluded amount: BigDecimal) extends AnyVal

final case class AnnWallet(redacted: AnnRedactedValueClass, excluded: AnnExcludedValueClass) derives Describe

final class AnnotationsSuite extends AnyFunSuite:

  private val singleLine: Configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = -1)

  test("@Redacted replaces the value with the default replacement"):
    assert(
      Describe[AnnDefaultRedaction].describe(AnnDefaultRedaction("s3cret", "bob"))(using singleLine) ==
        "AnnDefaultRedaction(password = <redacted>, name = \"bob\")"
    )

  test("@Redacted honours a custom replacement"):
    assert(
      Describe[AnnCustomRedaction].describe(AnnCustomRedaction("s3cret", "bob"))(using singleLine) ==
        "AnnCustomRedaction(password = ***, name = \"bob\")"
    )

  test("@Redacted with an explicitly named default replacement behaves like the implicit one"):
    assert(
      Describe[AnnNamedDefaultRedaction].describe(AnnNamedDefaultRedaction("s3cret"))(using singleLine) ==
        "AnnNamedDefaultRedaction(password = <redacted>)"
    )

  test("@Redacted never dereferences the value, so a null secret still renders"):
    assert(
      Describe[AnnDefaultRedaction].describe(AnnDefaultRedaction(null, "bob"))(using singleLine) ==
        "AnnDefaultRedaction(password = <redacted>, name = \"bob\")"
    )

  test("@Redacted still reports the declared type under useTypeNames"):
    assert(
      Describe[AnnDefaultRedaction].describe(AnnDefaultRedaction(null, "bob"))(using singleLine.copy(useTypeNames =
          true
        )) == "AnnDefaultRedaction(password: String = <redacted>, name: String = \"bob\")"
    )

  test("@Excluded omits the field entirely"):
    assert(
      Describe[AnnExcluded].describe(AnnExcluded("hidden", "bob"))(using singleLine) == "AnnExcluded(name = \"bob\")"
    )

  test("@Excluded never dereferences the value, so a null field still renders"):
    assert(Describe[AnnExcluded].describe(AnnExcluded(null, "bob"))(using singleLine) == "AnnExcluded(name = \"bob\")")

  test("@transient omits the field, exactly like @Excluded"):
    assert(
      Describe[AnnTransient].describe(AnnTransient("hidden", "bob"))(using singleLine) == "AnnTransient(name = \"bob\")"
    )

  test("@Redacted before @Excluded is omitted, not redacted"):
    assert(
      Describe[AnnRedactedThenExcluded].describe(AnnRedactedThenExcluded("s", "bob"))(using singleLine) ==
        "AnnRedactedThenExcluded(name = \"bob\")"
    )

  test("@Excluded before @Redacted is omitted as well, so source order is irrelevant"):
    assert(
      Describe[AnnExcludedThenRedacted].describe(AnnExcludedThenRedacted("s", "bob"))(using singleLine) ==
        "AnnExcludedThenRedacted(name = \"bob\")"
    )

  test("repeated @Redacted takes the first annotation in declaration order"):
    assert(
      Describe[AnnRepeatedRedaction].describe(AnnRepeatedRedaction("s"))(using singleLine) ==
        "AnnRepeatedRedaction(token = first)"
    )

  test("redaction composes through a nested case class"):
    assert(
      Describe[AnnHolder].describe(AnnHolder(AnnNestedSecret("hunter2", "t"), "x"))(using singleLine) ==
        "AnnHolder(inner = AnnNestedSecret(secret = <redacted>, tag = \"t\"), name = \"x\")"
    )

  test("redaction composes through a list element"):
    assert(
      Describe[AnnListHolder].describe(AnnListHolder(List(AnnNestedSecret("hunter2", "t"))))(using singleLine) ==
        "AnnListHolder(entries = [AnnNestedSecret(secret = <redacted>, tag = \"t\")])"
    )

  test("redaction composes through an Option payload"):
    assert(
      Describe[AnnOptionHolder].describe(AnnOptionHolder(Some(AnnNestedSecret("hunter2", "t"))))(using singleLine) ==
        "AnnOptionHolder(entry = Some(AnnNestedSecret(secret = <redacted>, tag = \"t\")))"
    )

  test("redaction composes through a map value"):
    assert(
      Describe[AnnMapHolder].describe(AnnMapHolder(Map("k" -> AnnNestedSecret("hunter2", "t"))))(using singleLine) ==
        "AnnMapHolder(entries = [\"k\" -> AnnNestedSecret(secret = <redacted>, tag = \"t\")])"
    )

  test("a redacted field is never dereferenced, even when its type could not be rendered at all"):
    assert(
      Describe[AnnNeverDereferenced].describe(AnnNeverDereferenced(null, null, "bob"))(using singleLine) ==
        "AnnNeverDereferenced(secret = <redacted>, name = \"bob\")"
    )

  test("a nested recursive type with no instance is rejected at compile time rather than inlined forever"):
    assertDoesNotCompile("Describe.derived[AnnUnrenderableHolder]")

  test("the same recursive type still derives when it is the root"):
    assertCompiles("Describe.derived[AnnUnrenderable]")

  test("@Redacted on the sole parameter of a value class replaces the payload"):
    assert(
      Describe[AnnWallet]
        .describe(AnnWallet(AnnRedactedValueClass(BigDecimal("1.50")), AnnExcludedValueClass(BigDecimal("2.50"))))(using
          singleLine)
        .startsWith("AnnWallet(redacted = <redacted>")
    )

  test("@Excluded on the sole parameter of a value class is ignored"):
    assert(
      Describe[AnnWallet]
        .describe(AnnWallet(AnnRedactedValueClass(BigDecimal("1.50")), AnnExcludedValueClass(BigDecimal("2.50"))))(using
          singleLine)
        .endsWith("excluded = 2.50)")
    )
