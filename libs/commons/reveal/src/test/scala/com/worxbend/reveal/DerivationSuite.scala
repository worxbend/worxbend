package com.worxbend.reveal

import com.worxbend.reveal.annotations.Redacted

import org.scalatest.funsuite.AnyFunSuite

final case class DerPoint(x: Int, y: Int) derives PrettyPrintable

final case class DerAwkwardNames(p: String, c: Int) derives PrettyPrintable

final case class DerShimmed(@Redacted password: String, name: String):
  override def toString: String = ToString.derived(this)

final case class DerCustom(value: Int)

object DerCustom:

  given PrettyPrintable[DerCustom] with
    override def describe(value: DerCustom)(using conf: Configuration): String = "CUSTOM"

final case class DerCustomHolder(inner: DerCustom, name: String) derives PrettyPrintable

final case class DerElement(value: Int)

object DerElement:

  /** In the implicit scope of `List[DerElement]`, which is what makes the ordering test meaningful. */
  given PrettyPrintable[List[DerElement]] with
    override def describe(value: List[DerElement])(using conf: Configuration): String = "CUSTOM-LIST"

  given PrettyPrintable[Option[DerElement]] with
    override def describe(value: Option[DerElement])(using conf: Configuration): String = "CUSTOM-OPTION"

final case class DerElementHolder(items: List[DerElement], entry: Option[DerElement]) derives PrettyPrintable

/** Entry points, ergonomics and the precedence of a user-written instance over structural inlining. */
final class DerivationSuite extends AnyFunSuite:

  private val singleLine: Configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = -1)

  test("derives PrettyPrintable puts an instance in the companion"):
    assert(summon[PrettyPrintable[DerPoint]].describe(DerPoint(1, 2))(using singleLine) == "DerPoint(x = 1, y = 2)")

  test("the asString extension renders with an explicit configuration"):
    assert(PrettyPrintable[DerPoint].asString(DerPoint(1, 2))(using singleLine) == "DerPoint(x = 1, y = 2)")

  test("the asString extension picks up an ambient configuration"):
    given configuration: Configuration = Configuration(fieldNameAndValueSeparator = ": ")
    assert(DerPoint(1, 2).asString == "DerPoint(x: 1, y: 2)")

  test("the asString extension falls back to the default configuration argument"):
    assert(DerPoint(1, 2).asString == "DerPoint(x = 1, y = 2)")

  test("the ToString shim falls back to the default configuration argument"):
    assert(ToString.derived(DerPoint(1, 2)) == "DerPoint(x = 1, y = 2)")

  test("Configuration's companion holds no given, so implicit search really can fail"):
    assertDoesNotCompile("summon[Configuration]")

  test("derivation tolerates fields named p and c"):
    assert(
      PrettyPrintable[DerAwkwardNames].describe(DerAwkwardNames("v", 3))(using singleLine) ==
        "DerAwkwardNames(p = \"v\", c = 3)"
    )

  test("the ToString shim renders at the call site"):
    assert(DerShimmed("s3cret", "bob").toString == "DerShimmed(password = <redacted>, name = \"bob\")")

  test("the ToString shim accepts an explicit configuration"):
    assert(ToString.derived(DerPoint(1, 2))(using Configuration(useFieldNames = false)) == "DerPoint(1, 2)")

  test("a user supplied instance wins over structural inlining"):
    assert(
      PrettyPrintable[DerCustomHolder].describe(DerCustomHolder(DerCustom(1), "n"))(using singleLine) ==
        "DerCustomHolder(inner = CUSTOM, name = \"n\")"
    )

  test("a user supplied instance for a collection type wins over the built in collection rendering"):
    assert(
      PrettyPrintable[DerElementHolder]
        .describe(DerElementHolder(List(DerElement(1)), Some(DerElement(2))))(using singleLine)
        .startsWith("DerElementHolder(items = CUSTOM-LIST")
    )

  test("a user supplied instance for an Option type wins over the built in Option rendering"):
    assert(
      PrettyPrintable[DerElementHolder]
        .describe(DerElementHolder(List(DerElement(1)), Some(DerElement(2))))(using singleLine)
        .endsWith("entry = CUSTOM-OPTION)")
    )

  test("a case class derives cleanly"):
    assertCompiles("PrettyPrintable.derived[DerPoint]")

  test("a non case class is rejected at compile time"):
    assertDoesNotCompile("PrettyPrintable.derived[Thread]")

  test("a plain trait is rejected at compile time"):
    assertDoesNotCompile("PrettyPrintable.derived[CharSequence]")

  test("a primitive is rejected at compile time"):
    assertDoesNotCompile("PrettyPrintable.derived[Int]")

/** A user-supplied instance must win for scalar types too, not only for structured ones.
  *
  * `renderPrimitive` used to run ahead of `renderSummoned`, which made the built-in `String`, `Char`, `Boolean` and
  * numeric renderings unoverridable while the README documented the opposite. These pin the documented contract.
  */
class ScalarOverrideSuite extends org.scalatest.funsuite.AnyFunSuite:

  private given Configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = -1)

  test("a user supplied instance wins over the built in String rendering"):
    given PrettyPrintable[String] with
      def describe(value: String)(using Configuration): String = s"<<$value>>"
    final case class T(s: String, i: Int) derives PrettyPrintable
    assert(summon[PrettyPrintable[T]].describe(T("v", 1)) == "T(s = <<v>>, i = 1)")

  test("a user supplied instance wins over the built in Int rendering"):
    given PrettyPrintable[Int] with
      def describe(value: Int)(using Configuration): String = s"#$value"
    final case class T(i: Int, s: String) derives PrettyPrintable
    assert(summon[PrettyPrintable[T]].describe(T(1, "v")) == """T(i = #1, s = "v")""")

  test("a user supplied scalar instance also applies inside a collection"):
    given PrettyPrintable[String] with
      def describe(value: String)(using Configuration): String = s"<<$value>>"
    final case class T(xs: List[String]) derives PrettyPrintable
    assert(summon[PrettyPrintable[T]].describe(T(List("a", "b"))) == "T(xs = [<<a>>, <<b>>])")

  test("without a user instance the built in scalar rendering is unchanged"):
    final case class T(s: String, i: Int) derives PrettyPrintable
    assert(summon[PrettyPrintable[T]].describe(T("v", 1)) == """T(s = "v", i = 1)""")
