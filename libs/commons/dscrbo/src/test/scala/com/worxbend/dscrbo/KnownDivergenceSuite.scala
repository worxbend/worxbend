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

  // Divergence 2 — generic case classes, and the exact condition under which they diverge.
  //
  // `derives Describe` on a generic type compiles. Summoning it at an instantiated type asks for a `Describe[A]`, and
  // this module ships no per-type instances by design — primitives are handled structurally inside the macro, so
  // there is no `Describe[Int]` to find and `DivergenceBox[Int]` fails. describo, whose typeclass has real instances
  // for primitives, handles the same shape unaided.
  //
  // The divergence is therefore the *missing instance*, not the generic. Supply one and the shape works, which the
  // second test pins so that nobody records this as a blanket incapability.
  test("a generic case class cannot be summoned at an instantiated type without an instance for the argument"):
    assertDoesNotCompile("summon[Describe[DivergenceBox[Int]]]")

  test("supplying an instance for the type argument makes the same generic shape work"):
    given Describe[Int] with
      def describe(value: Int)(using Configuration): String = value.toString
    assert(summon[Describe[DivergenceBox[Int]]].describe(DivergenceBox(
      1,
      "t",
    )) == """DivergenceBox(value = 1, tag = "t")""")

  test("the same shape works once the type parameter is gone"):
    assert(summon[Describe[DivergenceConcrete]].describe(DivergenceConcrete(
      1,
      "t",
    )) == "DivergenceConcrete(value = 1, tag = \"t\")")

  // Divergence 3 — nested case classes without their own instance.
  //
  // This module does not unroll a nested case class into its parent: it delegates to that type's own instance, and
  // renders with plain `toString` when there is none. describo, built on magnolia's AutoDerivation, derives the
  // nested type implicitly and renders it structurally. Both are deliberate; this one is the design of this module.
  // Shapes where the nested type *does* carry an instance agree, which is why the shared conformance kit still holds.
  test("a nested case class without its own instance renders with toString, unlike describo"):
    assert(
      summon[Describe[DivergenceHolder2]].describe(DivergenceHolder2(DivergencePlain(1, "v"))) ==
        "DivergenceHolder2(p = DivergencePlain(1,v))"
    )

  test("the same nested type renders structurally once it has an instance, which is where parity resumes"):
    given Describe[DivergencePlain] = Describe.derived[DivergencePlain]
    assert(
      Describe.derived[DivergenceHolder2].describe(DivergenceHolder2(DivergencePlain(1, "v"))) ==
        "DivergenceHolder2(p = DivergencePlain(a = 1, s = \"v\"))"
    )

final case class DivergencePlain(a: Int, s: String)

final case class DivergenceHolder2(p: DivergencePlain) derives Describe

enum DivergenceColour derives Describe:

  case Red
  case Green

final case class DivergenceHolder(c: DivergenceColour) derives Describe

final case class DivergenceBox[A](value: A, tag: String) derives Describe

final case class DivergenceConcrete(value: Int, tag: String) derives Describe
