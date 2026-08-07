package com.worxbend.dscrbo

import org.scalatest.funsuite.AnyFunSuite

/** The places where `dscrbo` and `describo` deliberately differ.
  *
  * The shared conformance kit pins everything the two renderers must agree on. This suite pins the short list of
  * things they do *not*, so each difference is visible from both sides and cannot quietly become three. Its twin lives
  * at `com.worxbend.describo.KnownDivergenceSuite`; changing one without the other should be uncomfortable.
  */
class KnownDivergenceSuite extends AnyFunSuite:

  private given Configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = -1)

  // Divergence 1 — enum case names under fullyQualifiedClassName.
  //
  // The macro reads the case's own symbol, whose owner is the enum, so the enum name is part of the qualified name.
  // Magnolia reports the enclosing *package* instead, so describo prints `com.worxbend.describo.Red`. dscrbo's
  // spelling is the more useful of the two; describo's is a limitation of Magnolia's TypeInfo, documented in its
  // README. Only qualified names are affected — simple names agree.
  test("an enum case's qualified name includes the enclosing enum"):
    given Configuration =
      Configuration(fullyQualifiedClassName = true, shortPackagePrefix = false, multilineIfFieldsAreGreaterOrEqual = -1)
    val actual          = summon[Describe[DivergenceHolder]].describe(DivergenceHolder(DivergenceColour.Red))
    assert(actual == "com.worxbend.dscrbo.DivergenceHolder(c = com.worxbend.dscrbo.DivergenceColour.Red)")

  test("an enum case's simple name agrees with describo"):
    assert(
      summon[Describe[DivergenceHolder]].describe(DivergenceHolder(DivergenceColour.Red)) == "DivergenceHolder(c = Red)"
    )

  // Divergence 2 — generic case classes.
  //
  // `derives Describe` on a generic type compiles, but the instance cannot be summoned at an instantiated type: the
  // synthesised `derived$Describe[A]` asks for a `Describe[A]`, and this module ships no per-type instances by design
  // — primitives are handled structurally inside the macro, so there is no `Describe[Int]` to find. describo, whose
  // typeclass has real instances for primitives, handles the same shape. Fixing this would mean giving dscrbo an
  // instance per built-in type, which is the design it exists to avoid.
  test("a generic case class cannot be summoned at an instantiated type"):
    assertDoesNotCompile("summon[Describe[DivergenceBox[Int]]]")

  test("the same shape works once the type parameter is gone"):
    assert(summon[Describe[DivergenceConcrete]].describe(DivergenceConcrete(
      1,
      "t",
    )) == "DivergenceConcrete(value = 1, tag = \"t\")")

enum DivergenceColour derives Describe:

  case Red
  case Green

final case class DivergenceHolder(c: DivergenceColour) derives Describe

final case class DivergenceBox[A](value: A, tag: String) derives Describe

final case class DivergenceConcrete(value: Int, tag: String) derives Describe
