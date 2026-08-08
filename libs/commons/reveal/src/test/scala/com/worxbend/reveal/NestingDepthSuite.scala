package com.worxbend.reveal

import scala.compiletime.testing.typeCheckErrors

import org.scalatest.funsuite.AnyFunSuite

// A plain chain of fourteen case classes, none of them carrying `derives Describe`. Under the current design this
// chain does not nest at all: the root expands, and its single field — an ordinary nested case class with no instance
// — renders with its own `toString`. Depth is therefore irrelevant to it, which is the point of the fixtures below.
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

// Sealed families are the shape that still expands structurally wherever it appears, because a branch has no instance
// of its own to delegate to. Each level here costs one type and two emitted layers — one for the dispatch, one for
// descending into the branch's field — so the twenty-layer cap binds before the twelve-type one. Sealed07 is ten
// levels deep and exactly at the cap; Sealed06 is one level further.
sealed trait Sealed16
final case class Branch16(value: String)   extends Sealed16
sealed trait Sealed15
final case class Branch15(value: Sealed16) extends Sealed15
sealed trait Sealed14
final case class Branch14(value: Sealed15) extends Sealed14
sealed trait Sealed13
final case class Branch13(value: Sealed14) extends Sealed13
sealed trait Sealed12
final case class Branch12(value: Sealed13) extends Sealed12
sealed trait Sealed11
final case class Branch11(value: Sealed12) extends Sealed11
sealed trait Sealed10
final case class Branch10(value: Sealed11) extends Sealed10
sealed trait Sealed09
final case class Branch09(value: Sealed10) extends Sealed09
sealed trait Sealed08
final case class Branch08(value: Sealed09) extends Sealed08
sealed trait Sealed07
final case class Branch07(value: Sealed08) extends Sealed07
sealed trait Sealed06
final case class Branch06(value: Sealed07) extends Sealed06

/** What the macro expands, what it delegates, and where the caps on the expanding shapes sit.
  *
  * The caps exist because expansion is recursion in the macro rather than in the compiler's inliner, so nothing bounds
  * it automatically. Since nested case classes stopped being unrolled, the caps only ever bind on the shapes that
  * still expand — sealed families, value classes, and wrapper chains — which is a far smaller surface than before.
  */
final class NestingDepthSuite extends AnyFunSuite:

  private val singleLine: Configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = -1)

  // ---------------------------------------------------- plain case classes

  test("a fourteen deep chain of plain case classes derives, because none of it is nested into the root"):
    assertCompiles("Describe.derived[Plain00]")

  test("only the root of that chain is structured; the rest is its own toString"):
    val value = Plain00(
      Plain01(
        Plain02(Plain03(Plain04(Plain05(Plain06(Plain07(Plain08(Plain09(Plain10(Plain11(Plain12(Plain13("x"))))))))))))
      )
    )
    assert(
      Describe.derived[Plain00].describe(value)(using singleLine) ==
        "Plain00(value = Plain01(Plain02(Plain03(Plain04(Plain05(Plain06(Plain07(Plain08(Plain09(Plain10(Plain11(Plain12(Plain13(x))))))))))))))"
    )

  // ------------------------------------------------------- sealed families

  test("ten levels of sealed dispatch expand"):
    assertCompiles("Describe.derived[Sealed07]")

  test("eleven levels of sealed dispatch are refused"):
    assert(typeCheckErrors("Describe.derived[Sealed06]").nonEmpty)

  // The refusal names the branch it stopped at rather than the sealed trait above it, because the branch is the type
  // being expanded when the budget runs out and is therefore the one an instance has to be attached to.
  test("the sealed refusal is the layer cap, and it names the branch it stopped at"):
    val errors = typeCheckErrors("Describe.derived[Sealed06]").map(_.message)
    assert(errors.exists(message => message.contains("layers of generated code")), errors.mkString("\n"))
    assert(errors.exists(message => message.contains("Branch")), errors.mkString("\n"))

  test("the sealed refusal prescribes a remedy implicit search really honours"):
    val errors = typeCheckErrors("Describe.derived[Sealed06]").map(_.message)
    assert(errors.exists(message => message.contains("derives Describe")), errors.mkString("\n"))

  test("giving one link of a too deep sealed chain its own instance breaks the chain"):
    assertCompiles("""
      given Describe[Sealed10] = Describe.derived[Sealed10]
      Describe.derived[Sealed06]
    """)

  test("a ten level sealed chain renders through every level"):
    val value: Sealed07 =
      Branch07(Branch08(Branch09(Branch10(Branch11(Branch12(Branch13(Branch14(Branch15(Branch16("x"))))))))))
    assert(
      Describe
        .derived[Sealed07]
        .describe(value)(using singleLine)
        .startsWith("Branch07(value = Branch08(value = Branch09(")
    )
