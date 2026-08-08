package com.worxbend.reveal

import org.scalatest.funsuite.AnyFunSuite

/** How this module spells qualified names, and what it needs before a generic case class will derive.
  *
  * Both are easy to change by accident and neither is covered elsewhere, so they are pinned here.
  */
class NamingAndGenericsSuite extends AnyFunSuite:

  private given Configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = -1)

  // The macro reads the enum case's own symbol, whose owner is the enum, so the enum name is part of the qualified
  // name. Only qualified names are affected — simple names are unremarkable.
  test("an enum case's qualified name includes the enclosing enum"):
    given Configuration =
      Configuration(fullyQualifiedClassName = true, shortPackagePrefix = false, multilineIfFieldsAreGreaterOrEqual = -1)
    val actual          = summon[Describe[DivergenceHolder]].describe(DivergenceHolder(DivergenceColour.Red))
    assert(actual == "com.worxbend.reveal.DivergenceHolder(c = com.worxbend.reveal.DivergenceColour.Red)")

  test("an enum case's simple name carries no package or enum prefix"):
    assert(
      summon[Describe[DivergenceHolder]].describe(DivergenceHolder(DivergenceColour.Red)) == "DivergenceHolder(c = Red)"
    )

  // `derives Describe` on a generic type compiles. Summoning it at an instantiated type asks for a `Describe[A]`,
  // and this module ships no per-type instances by design — primitives are handled structurally inside the macro, so
  // there is no `Describe[Int]` to find. Supply one and the shape works, which the second test pins so that nobody
  // records this as a blanket incapability.
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

  // Nested case classes are not unrolled: they delegate to their own instance, or render with plain `toString`.
  // DelegationSuite is the full treatment; these two keep the qualified-name fixtures honest alongside it.
  test("a nested case class without its own instance renders with toString"):
    assert(
      summon[Describe[DivergenceHolder2]].describe(DivergenceHolder2(DivergencePlain(1, "v"))) ==
        "DivergenceHolder2(p = DivergencePlain(1,v))"
    )

  test("the same nested type renders structurally once it has an instance, which is the documented way to opt in"):
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
