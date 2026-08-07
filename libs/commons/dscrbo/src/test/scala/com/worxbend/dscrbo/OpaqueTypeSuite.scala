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

/** Concrete final classes the macro cannot see into are still safe: nothing else can be substituted for them. */
final case class OpqConcreteHolder(day: LocalDate, error: Throwable) derives Describe

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

  test("a concrete final class is still rendered by its own toString"):
    assert(
      Describe[OpqConcreteHolder]
        .describe(OpqConcreteHolder(LocalDate.parse("2023-01-01"), RuntimeException("boom")))(using singleLine) ==
        "OpqConcreteHolder(day = 2023-01-01, error = java.lang.RuntimeException: boom)"
    )
