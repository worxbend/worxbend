package com.worxbend.dscrbo

import com.worxbend.dscrbo.annotations.Excluded
import com.worxbend.dscrbo.annotations.Redacted

import scala.compiletime.testing.typeCheckErrors

import java.time.LocalDate

import org.scalatest.funsuite.AnyFunSuite

/** A non-sealed trait: the runtime value may be any subtype, including one with secrets. */
trait OpqAnimal

final case class OpqDog(@Redacted chip: String, name: String) extends OpqAnimal

final case class OpqAnimalHolder(animal: OpqAnimal)

final case class OpqAnyHolder(value: Any)

abstract class OpqShape

final case class OpqShapeHolder(shape: OpqShape)

/** The same shape, but with the instance the error message asks for. */
trait OpqPlant

object OpqPlant:

  given Describe[OpqPlant] with
    override def describe(value: OpqPlant)(using conf: Configuration): String = "PLANT"

final case class OpqFern(name: String) extends OpqPlant

final case class OpqPlantHolder(plant: OpqPlant) derives Describe

final case class OpqExcludedAnimalHolder(@Excluded animal: OpqAnimal, name: String) derives Describe

/** Concrete classes the macro cannot see into are rendered by their own `toString`.
  *
  * Note what this does and does not claim. `LocalDate` is final, so nothing can be substituted for it. `Throwable` is
  * *not* final, so a subclass carrying `@Redacted` fields is legal and would be printed in the clear — see the
  * non-final hole recorded on `isAbstractlyTyped`. This fixture pins the current, deliberate behaviour; it is not
  * evidence that the behaviour is safe for every non-final type.
  */
final case class OpqConcreteHolder(day: LocalDate, error: Throwable) derives Describe

/** Composite declared types that pin down no runtime class at all.
  *
  * All three defeated the fail-closed gate by making its flag tests vacuous: an intersection and a union both have
  * `NoSymbol` for a `typeSymbol`, and a refinement's `typeSymbol` is the alias symbol of the refined type, which
  * carries none of the parent's flags. Each of these therefore reached `Rendering.opaque` and printed a `@Redacted`
  * field in the clear. They are matched explicitly now, and pinned below so the refusal cannot silently regress.
  */
final case class OpqIntersectionHolder(value: OpqAnimal & java.io.Serializable)

final case class OpqUnionHolder(value: OpqAnimal | String)

final case class OpqRefinementHolder(value: AnyRef { def foo: Int })

/** What happens at a field whose declared type does not pin down the runtime class. */
final class OpaqueTypeSuite extends AnyFunSuite:

  private val singleLine: Configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = -1)

  test("a field typed as a non sealed trait is refused rather than rendered with toString"):
    assertDoesNotCompile("Describe.derived[OpqAnimalHolder]")

  test("the refusal names the type the macro cannot see into"):
    val errors = typeCheckErrors("Describe.derived[OpqAnimalHolder]")
    assert(errors.exists(error => error.message.contains("OpqAnimal")))

  test("a field typed as Any is refused"):
    assertDoesNotCompile("Describe.derived[OpqAnyHolder]")

  test("a field typed as an abstract class is refused"):
    assertDoesNotCompile("Describe.derived[OpqShapeHolder]")

  test("providing the instance the error asks for makes the same shape compile"):
    assert(Describe[OpqPlantHolder].describe(OpqPlantHolder(OpqFern("f")))(using
      singleLine) == "OpqPlantHolder(plant = PLANT)")

  test("excluding the field also makes the same shape compile, because the value is never read"):
    assert(
      Describe[OpqExcludedAnimalHolder].describe(OpqExcludedAnimalHolder(OpqDog("s3cret", "rex"), "kennel"))(using
        singleLine) == "OpqExcludedAnimalHolder(name = \"kennel\")"
    )

  test("a field typed as an intersection is refused, because no runtime class is pinned down"):
    assertDoesNotCompile("Describe.derived[OpqIntersectionHolder]")

  test("a field typed as a union is refused, because no runtime class is pinned down"):
    assertDoesNotCompile("Describe.derived[OpqUnionHolder]")

  test("a field typed as a refinement is refused whenever its refined parent is"):
    assertDoesNotCompile("Describe.derived[OpqRefinementHolder]")

  // assertDoesNotCompile alone would also pass if the shape failed for some unrelated reason, which would leave the
  // leak un-pinned. These assert the refusal is the fail-closed one, so the gate is what is being tested.
  test("the intersection refusal is the fail closed one, not an unrelated compile error"):
    val errors = typeCheckErrors("Describe.derived[OpqIntersectionHolder]")
    assert(errors.exists(error => error.message.contains("the declared type is abstract")))

  test("the union refusal is the fail closed one, not an unrelated compile error"):
    val errors = typeCheckErrors("Describe.derived[OpqUnionHolder]")
    assert(errors.exists(error => error.message.contains("the declared type is abstract")))

  test("the refinement refusal is the fail closed one, not an unrelated compile error"):
    val errors = typeCheckErrors("Describe.derived[OpqRefinementHolder]")
    assert(errors.exists(error => error.message.contains("the declared type is abstract")))

  test("a concrete final class is still rendered by its own toString"):
    assert(
      Describe[OpqConcreteHolder]
        .describe(OpqConcreteHolder(LocalDate.parse("2023-01-01"), RuntimeException("boom")))(using singleLine) ==
        "OpqConcreteHolder(day = 2023-01-01, error = java.lang.RuntimeException: boom)"
    )
