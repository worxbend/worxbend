package com.worxbend.describo

import org.scalatest.funsuite.AnyFunSuite

/** How this module spells qualified names, and how it handles generic and nested case classes.
  *
  * All three are easy to change by accident and none is covered elsewhere, so they are pinned here.
  */
class NamingAndGenericsSuite extends AnyFunSuite:

  private given Configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = -1)

  // Magnolia's TypeInfo reports an enum case's owner as the enclosing *package*, not the enclosing enum, so the enum
  // name is missing from the qualified spelling. Threading the parent's name through `split` into every child would
  // change the `Printable` interface for a spelling that only appears under a non-default flag, so it stays as it is.
  // Only qualified names are affected — simple names are unremarkable.
  test("an enum case's qualified name omits the enclosing enum"):
    given Configuration =
      Configuration(fullyQualifiedClassName = true, shortPackagePrefix = false, multilineIfFieldsAreGreaterOrEqual = -1)
    val actual          = summon[Printable[DivergenceHolder]].asString(DivergenceHolder(DivergenceColour.Red))
    assert(actual == "com.worxbend.describo.DivergenceHolder(c = com.worxbend.describo.Red)")

  test("an enum case's simple name carries no package or enum prefix"):
    assert(
      summon[Printable[DivergenceHolder]].asString(DivergenceHolder(
        DivergenceColour.Red
      )) == "DivergenceHolder(c = Red)"
    )

  // This module derives a generic case class at the instantiated type without ceremony, because its typeclass has
  // real instances for the built-in types a type parameter resolves to.
  test("a generic case class derives at the instantiated type"):
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

  // Magnolia's AutoDerivation derives a nested case class implicitly, so this module renders it structurally whether
  // or not the author asked for an instance.
  test("a nested case class without its own instance is still auto-derived"):
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
