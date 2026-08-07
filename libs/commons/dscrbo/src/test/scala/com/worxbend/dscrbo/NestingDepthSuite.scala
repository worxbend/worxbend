package com.worxbend.dscrbo

import scala.compiletime.testing.typeCheckErrors

import org.scalatest.funsuite.AnyFunSuite

// A plain chain of fourteen case classes, deliberately without `derives Describe` so that nothing short-circuits the
// structural inlining. Plain01 is exactly at the twelve-type cap; Plain00 is one type past it.
final case class Plain13(value: String)
final case class Plain12(value: Plain13)
final case class Plain11(value: Plain12)
final case class Plain10(value: Plain11)
final case class Plain09(value: Plain10)
final case class Plain08(value: Plain09)
final case class Plain07(value: Plain08)
final case class Plain06(value: Plain07)
final case class Plain05(value: Plain06)
final case class Plain04(value: Plain05)
final case class Plain03(value: Plain04)
final case class Plain02(value: Plain03)
final case class Plain01(value: Plain02)
final case class Plain00(value: Plain01)

// The same idea, but each level goes through three wrappers, so each level costs four layers of generated code rather
// than one. Wrap01 is five types deep and exactly at the twenty-layer cap; Wrap00 is one type further.
final case class Wrap06(value: String)
final case class Wrap05(value: Option[Map[String, List[Wrap06]]])
final case class Wrap04(value: Option[Map[String, List[Wrap05]]])
final case class Wrap03(value: Option[Map[String, List[Wrap04]]])
final case class Wrap02(value: Option[Map[String, List[Wrap03]]])
final case class Wrap01(value: Option[Map[String, List[Wrap02]]])
final case class Wrap00(value: Option[Map[String, List[Wrap01]]])

/** The two nesting caps: where each sits, what each says, and how to get past them. */
final class NestingDepthSuite extends AnyFunSuite:

  private val singleLine: Configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = -1)

  test("twelve types of nesting below the root derive"):
    assertCompiles("Describe.derived[Plain01]")

  test("thirteen types of nesting below the root are refused"):
    assertDoesNotCompile("Describe.derived[Plain00]")

  test("the refusal names the type that is actually too deep, not the leaf it happened to reach"):
    val errors = typeCheckErrors("Describe.derived[Plain00]")
    assert(errors.exists(error => error.message.contains("Plain13")))

  test("the refusal does not blame the String at the bottom of the chain"):
    val errors = typeCheckErrors("Describe.derived[Plain00]")
    assert(errors.forall(error => !error.message.contains("java.lang.String")))

  test("providing the instance the refusal asks for breaks the chain"):
    assertCompiles("{ given Describe[Plain07] = Describe.derived[Plain07]; Describe.derived[Plain00] }")

  test("wrappers do not consume a type level, so five heavily wrapped types still derive"):
    assertCompiles("Describe.derived[Wrap01]")

  test("wrappers do consume a layer, so one type further is refused before the compiler stack can overflow"):
    assertDoesNotCompile("Describe.derived[Wrap00]")

  test("the layer refusal explains that wrappers cost layers and names the type it stopped at"):
    val errors = typeCheckErrors("Describe.derived[Wrap00]")
    assert(errors.exists(error =>
      error.message.contains("layers of generated code") && error.message.contains("Wrap06")
    ))

  test("a heavily wrapped chain renders through every wrapper"):
    val value = Wrap05(Some(Map("k" -> List(Wrap06("x")))))
    assert(
      Describe.derived[Wrap05].describe(value)(using singleLine) ==
        "Wrap05(value = Some([\"k\" -> [Wrap06(value = \"x\")]]))"
    )
