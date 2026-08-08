package com.worxbend.dscrbo

import com.worxbend.dscrbo.annotations.Redacted

import org.scalatest.funsuite.AnyFunSuite

final case class DerPoint(x: Int, y: Int) derives Describe

final case class DerAwkwardNames(p: String, c: Int) derives Describe

final case class DerShimmed(@Redacted password: String, name: String):
  override def toString: String = ToString.derived(this)

final case class DerCustom(value: Int)

object DerCustom:

  given Describe[DerCustom] with
    override def describe(value: DerCustom)(using conf: Configuration): String = "CUSTOM"

final case class DerCustomHolder(inner: DerCustom, name: String) derives Describe

final case class DerElement(value: Int)

object DerElement:

  /** In the implicit scope of `List[DerElement]`, which is what makes the ordering test meaningful. */
  given Describe[List[DerElement]] with
    override def describe(value: List[DerElement])(using conf: Configuration): String = "CUSTOM-LIST"

  given Describe[Option[DerElement]] with
    override def describe(value: Option[DerElement])(using conf: Configuration): String = "CUSTOM-OPTION"

final case class DerElementHolder(items: List[DerElement], entry: Option[DerElement]) derives Describe

/** Entry points, ergonomics and the precedence of a user-written instance over structural inlining. */
final class DerivationSuite extends AnyFunSuite:

  private val singleLine: Configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = -1)

  test("derives Describe puts an instance in the companion"):
    assert(summon[Describe[DerPoint]].describe(DerPoint(1, 2))(using singleLine) == "DerPoint(x = 1, y = 2)")

  test("the asString extension renders with an explicit configuration"):
    assert(Describe[DerPoint].asString(DerPoint(1, 2))(using singleLine) == "DerPoint(x = 1, y = 2)")

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
      Describe[DerAwkwardNames].describe(DerAwkwardNames("v", 3))(using singleLine) ==
        "DerAwkwardNames(p = \"v\", c = 3)"
    )

  test("the ToString shim renders at the call site"):
    assert(DerShimmed("s3cret", "bob").toString == "DerShimmed(password = <redacted>, name = \"bob\")")

  test("the ToString shim accepts an explicit configuration"):
    assert(ToString.derived(DerPoint(1, 2))(using Configuration(useFieldNames = false)) == "DerPoint(1, 2)")

  test("a user supplied instance wins over structural inlining"):
    assert(
      Describe[DerCustomHolder].describe(DerCustomHolder(DerCustom(1), "n"))(using singleLine) ==
        "DerCustomHolder(inner = CUSTOM, name = \"n\")"
    )

  test("a user supplied instance for a collection type wins over the built in collection rendering"):
    assert(
      Describe[DerElementHolder]
        .describe(DerElementHolder(List(DerElement(1)), Some(DerElement(2))))(using singleLine)
        .startsWith("DerElementHolder(items = CUSTOM-LIST")
    )

  test("a user supplied instance for an Option type wins over the built in Option rendering"):
    assert(
      Describe[DerElementHolder]
        .describe(DerElementHolder(List(DerElement(1)), Some(DerElement(2))))(using singleLine)
        .endsWith("entry = CUSTOM-OPTION)")
    )

  test("a case class derives cleanly"):
    assertCompiles("Describe.derived[DerPoint]")

  test("a non case class is rejected at compile time"):
    assertDoesNotCompile("Describe.derived[Thread]")

  test("a plain trait is rejected at compile time"):
    assertDoesNotCompile("Describe.derived[CharSequence]")

  test("a primitive is rejected at compile time"):
    assertDoesNotCompile("Describe.derived[Int]")

/** A user-supplied instance must win for scalar types too, not only for structured ones.
  *
  * `renderPrimitive` used to run ahead of `renderSummoned`, which made the built-in `String`, `Char`, `Boolean` and
  * numeric renderings unoverridable while the README documented the opposite. These pin the documented contract.
  */
class ScalarOverrideSuite extends org.scalatest.funsuite.AnyFunSuite:

  private given Configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = -1)

  test("a user supplied instance wins over the built in String rendering"):
    given Describe[String] with
      def describe(value: String)(using Configuration): String = s"<<$value>>"
    final case class T(s: String, i: Int) derives Describe
    assert(summon[Describe[T]].describe(T("v", 1)) == "T(s = <<v>>, i = 1)")

  test("a user supplied instance wins over the built in Int rendering"):
    given Describe[Int] with
      def describe(value: Int)(using Configuration): String = s"#$value"
    final case class T(i: Int, s: String) derives Describe
    assert(summon[Describe[T]].describe(T(1, "v")) == """T(i = #1, s = "v")""")

  test("a user supplied scalar instance also applies inside a collection"):
    given Describe[String] with
      def describe(value: String)(using Configuration): String = s"<<$value>>"
    final case class T(xs: List[String]) derives Describe
    assert(summon[Describe[T]].describe(T(List("a", "b"))) == "T(xs = [<<a>>, <<b>>])")

  test("without a user instance the built in scalar rendering is unchanged"):
    final case class T(s: String, i: Int) derives Describe
    assert(summon[Describe[T]].describe(T("v", 1)) == """T(s = "v", i = 1)""")
