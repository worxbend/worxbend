package com.worxbend.describo

import com.worxbend.describo.annotations.Redacted

import org.scalatest.funsuite.AnyFunSuite

/** The mixin resolves its `Configuration` at the class-definition site, so every fixture in one lexical scope shares
  * one configuration. Fixtures that need a different one live in their own object.
  */
object MixinDefaults:

  given configuration: Configuration = Configuration.default

  final case class Account(@Redacted password: String, name: String) extends AutoToString derives Printable

  /** D1 regression: `AutoToString` used to declare `given p` and `given c`, which collided with any field of the same
    * name and failed to compile.
    */
  final case class Coordinates(p: Int, c: String) extends AutoToString derives Printable

object MixinCustomised:

  given configuration: Configuration = Configuration(useFieldNames = false, fieldsSeparator = " | ")

  final case class Point(x: Int, y: Int) extends AutoToString derives Printable

class AutoToStringSuite extends AnyFunSuite:

  test("the mixin replaces toString with the derived rendering"):
    assert(MixinDefaults.Account("secret", "bob").toString == "Account(password = <redacted>, name = \"bob\")")

  test("a case class with a field named p compiles and renders it"):
    assert(MixinDefaults.Coordinates(1, "a").toString.contains("p = 1"))

  test("a case class with a field named c compiles and renders it"):
    assert(MixinDefaults.Coordinates(1, "a").toString.contains("c = \"a\""))

  test("the mixin honours the configuration in scope at the class-definition site"):
    assert(MixinCustomised.Point(1, 2).toString == "Point(1 | 2)")

  test("string interpolation of a mixin instance uses the derived rendering"):
    assert(s"${MixinCustomised.Point(1, 2)}" == "Point(1 | 2)")
