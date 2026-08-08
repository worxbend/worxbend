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
    val actual          = summon[PrettyPrintable[DivergenceHolder]].describe(DivergenceHolder(DivergenceColour.Red))
    assert(actual == "com.worxbend.reveal.DivergenceHolder(c = com.worxbend.reveal.DivergenceColour.Red)")

  test("an enum case's simple name carries no package or enum prefix"):
    assert(
      summon[PrettyPrintable[DivergenceHolder]].describe(DivergenceHolder(
        DivergenceColour.Red
      )) == "DivergenceHolder(c = Red)"
    )

  // `derives PrettyPrintable` on a generic type compiles. Summoning it at an instantiated type asks for a `PrettyPrintable[A]`,
  // and this module ships no per-type instances by design — primitives are handled structurally inside the macro, so
  // there is no `PrettyPrintable[Int]` to find. Supply one and the shape works, which the second test pins so that nobody
  // records this as a blanket incapability.
  test("a generic case class cannot be summoned at an instantiated type without an instance for the argument"):
    assertDoesNotCompile("summon[PrettyPrintable[DivergenceBox[Int]]]")

  test("supplying an instance for the type argument makes the same generic shape work"):
    given PrettyPrintable[Int] with
      def describe(value: Int)(using Configuration): String = value.toString
    assert(summon[PrettyPrintable[DivergenceBox[Int]]].describe(DivergenceBox(
      1,
      "t",
    )) == """DivergenceBox(value = 1, tag = "t")""")

  test("the same shape works once the type parameter is gone"):
    assert(summon[PrettyPrintable[DivergenceConcrete]].describe(DivergenceConcrete(
      1,
      "t",
    )) == "DivergenceConcrete(value = 1, tag = \"t\")")

  // Nested case classes are not unrolled: they delegate to their own instance, or render with plain `toString`.
  // DelegationSuite is the full treatment; these two keep the qualified-name fixtures honest alongside it.
  test("a nested case class without its own instance renders with toString"):
    assert(
      summon[PrettyPrintable[DivergenceHolder2]].describe(DivergenceHolder2(DivergencePlain(1, "v"))) ==
        "DivergenceHolder2(p = DivergencePlain(1,v))"
    )

  test("the same nested type renders structurally once it has an instance, which is the documented way to opt in"):
    given PrettyPrintable[DivergencePlain] = PrettyPrintable.derived[DivergencePlain]
    assert(
      PrettyPrintable.derived[DivergenceHolder2].describe(DivergenceHolder2(DivergencePlain(1, "v"))) ==
        "DivergenceHolder2(p = DivergencePlain(a = 1, s = \"v\"))"
    )

final case class DivergencePlain(a: Int, s: String)

final case class DivergenceHolder2(p: DivergencePlain) derives PrettyPrintable

enum DivergenceColour derives PrettyPrintable:

  case Red
  case Green

final case class DivergenceHolder(c: DivergenceColour) derives PrettyPrintable

final case class DivergenceBox[A](value: A, tag: String) derives PrettyPrintable

final case class DivergenceConcrete(value: Int, tag: String) derives PrettyPrintable
