package com.worxbend.describo

import org.scalatest.funsuite.AnyFunSuite

/** The places where `describo` and `dscrbo` deliberately differ.
  *
  * The shared conformance kit pins everything the two renderers must agree on. This suite pins the short list of
  * things they do *not*, so each difference is visible from both sides and cannot quietly become three. Its twin lives
  * at `com.worxbend.dscrbo.KnownDivergenceSuite`; changing one without the other should be uncomfortable.
  */
class KnownDivergenceSuite extends AnyFunSuite:

  private given Configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = -1)

  // Divergence 1 — enum case names under fullyQualifiedClassName.
  //
  // Magnolia's TypeInfo reports an enum case's owner as the enclosing *package*, not the enclosing enum, so the enum
  // name is missing from the qualified spelling. The sibling macro reads the case symbol directly and prints
  // `com.worxbend.dscrbo.DivergenceColour.Red`, which is the more useful of the two. Fixing this here would mean
  // threading the parent's name through `split` into every child's instance, changing the `Printable` interface for a
  // spelling that only appears under a non-default flag. Documented in the README instead.
  //
  // Only qualified names are affected — simple names agree, which is what the conformance kit exercises.
  test("an enum case's qualified name omits the enclosing enum, unlike dscrbo"):
    given Configuration =
      Configuration(fullyQualifiedClassName = true, shortPackagePrefix = false, multilineIfFieldsAreGreaterOrEqual = -1)
    val actual          = summon[Printable[DivergenceHolder]].asString(DivergenceHolder(DivergenceColour.Red))
    assert(actual == "com.worxbend.describo.DivergenceHolder(c = com.worxbend.describo.Red)")

  test("an enum case's simple name agrees with dscrbo"):
    assert(
      summon[Printable[DivergenceHolder]].asString(DivergenceHolder(
        DivergenceColour.Red
      )) == "DivergenceHolder(c = Red)"
    )

  // Divergence 2 — generic case classes.
  //
  // This module derives a generic case class at the instantiated type without ceremony, because its typeclass has real
  // instances for the built-in types a type parameter resolves to. The sibling macro cannot: it ships no per-type
  // instances by design, so `Describe[Int]` does not exist for its synthesised `derived$Describe[A]` to find.
  test("a generic case class derives at the instantiated type, unlike dscrbo"):
    assert(summon[Printable[DivergenceBox[Int]]].asString(DivergenceBox(
      1,
      "t",
    )) == "DivergenceBox(value = 1, tag = \"t\")")

  test("the same generic type also renders at another instantiation"):
    assert(
      summon[Printable[DivergenceBox[String]]].asString(DivergenceBox(
        "v",
        "t",
      )) == "DivergenceBox(value = \"v\", tag = \"t\")"
    )

  // Divergence 3 — nested case classes without their own instance.
  //
  // Magnolia's AutoDerivation derives a nested case class implicitly, so this module renders it structurally whether
  // or not the author asked for an instance. The sibling macro deliberately does not: it delegates to the nested
  // type's own instance and falls back to plain `toString` when there is none. Shapes where the nested type does
  // carry an instance agree, which is what keeps the shared conformance kit meaningful.
  test("a nested case class without its own instance is still auto-derived, unlike dscrbo"):
    assert(
      summon[Printable[DivergenceHolder2]].asString(DivergenceHolder2(DivergencePlain(1, "v"))) ==
        "DivergenceHolder2(p = DivergencePlain(a = 1, s = \"v\"))"
    )

final case class DivergencePlain(a: Int, s: String)

final case class DivergenceHolder2(p: DivergencePlain) derives Printable

enum DivergenceColour derives Printable:

  case Red
  case Green

final case class DivergenceHolder(c: DivergenceColour) derives Printable

final case class DivergenceBox[A](value: A, tag: String) derives Printable
