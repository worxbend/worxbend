package com.worxbend.reveal

import com.worxbend.reveal.annotations.Redacted

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import org.scalatest.funsuite.AnyFunSuite

/** Carries its own instance, because nested case classes are not inlined into their parent.
  *
  * It used to be declared without one, back when the macro unrolled everything it could reach. Now a redacting case
  * class reached as a field is a compile error rather than a silent `toString`, so the annotation is what forces the
  * `derives` here — which is exactly the pressure the design intends.
  */
final case class GenSecret(@Redacted token: String, tag: String) derives PrettyPrintable

final case class GenEitherHolder(value: Either[String, GenSecret]) derives PrettyPrintable

final case class GenTryHolder(value: Try[Int]) derives PrettyPrintable

final case class GenOptionEitherHolder(value: Option[Either[String, Int]]) derives PrettyPrintable

final case class GenListEitherHolder(value: List[Either[String, Int]]) derives PrettyPrintable

sealed trait GenResult[A]

object GenResult:

  final case class Ok[A](value: A)      extends GenResult[A]
  final case class Bad[A](note: String) extends GenResult[A]

final case class GenResultHolder(value: GenResult[String]) derives PrettyPrintable

enum GenPair[A]:

  case One(value: A)
  case Two(first: A, second: A)

final case class GenPairHolder(value: GenPair[String]) derives PrettyPrintable

enum GenTree[+A]:

  case Leaf(value: A)
  case Empty

final case class GenTreeHolder(value: GenTree[String]) derives PrettyPrintable

sealed trait GenTop

object GenTop:

  sealed trait GenMid                                          extends GenTop
  final case class GenBottom(@Redacted s: String, tag: String) extends GenMid

final case class GenTopHolder(value: GenTop) derives PrettyPrintable

/** Sealed families whose children carry type parameters. The children have to be instantiated at the parent's type
  * arguments; using the uninstantiated child reference makes the compiler fail inside its own inliner.
  */
final class SealedFamilySuite extends AnyFunSuite:

  private val singleLine: Configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = -1)

  test("Either dispatches to its right branch"):
    assert(
      PrettyPrintable[GenEitherHolder].describe(GenEitherHolder(Right(GenSecret("s3cret", "t"))))(using singleLine) ==
        "GenEitherHolder(value = Right(value = GenSecret(token = <redacted>, tag = \"t\")))"
    )

  test("redaction composes through an Either branch"):
    assert(
      !PrettyPrintable[GenEitherHolder]
        .describe(GenEitherHolder(Right(GenSecret("s3cret", "t"))))(using singleLine)
        .contains("s3cret")
    )

  test("Either dispatches to its left branch"):
    assert(
      PrettyPrintable[GenEitherHolder].describe(GenEitherHolder(Left("boom")))(using singleLine) ==
        "GenEitherHolder(value = Left(value = \"boom\"))"
    )

  test("Either derives at the root"):
    assert(PrettyPrintable.derived[Either[String, Int]].describe(Right(1))(using singleLine) == "Right(value = 1)")

  test("Try dispatches to Success"):
    assert(
      PrettyPrintable[GenTryHolder].describe(GenTryHolder(Success(1)))(using singleLine) ==
        "GenTryHolder(value = Success(value = 1))"
    )

  // `Throwable` is concrete but NOT final, so this is the acceptance rule at its weakest point: the macro renders any
  // non-abstract type by `toString`, and `isAbstractlyTyped` tests only for abstract types, traits, abstract classes
  // and the universal types. Finality is not checked anywhere, so a non-final class is accepted here on the strength
  // of being concrete alone — not, as an earlier comment claimed, because nothing can be substituted for it.
  //
  // The gap that leaves is recorded on `isAbstractlyTyped` itself: a subtype carrying `@Redacted` would have its
  // annotation ignored by `toString`. Closing it means refusing every non-final class, which would refuse `Throwable`
  // and take `Try`'s Failure payload with it, so it is a specification decision rather than an oversight.
  test("Try dispatches to Failure, whose payload is a concrete non-final class rendered by toString"):
    assert(
      PrettyPrintable[GenTryHolder].describe(GenTryHolder(Failure(RuntimeException("boom"))))(using singleLine) ==
        "GenTryHolder(value = Failure(exception = java.lang.RuntimeException: boom))"
    )

  test("a generic Option payload dispatches through the family"):
    assert(
      PrettyPrintable[GenOptionEitherHolder].describe(GenOptionEitherHolder(Some(Left("x"))))(using singleLine) ==
        "GenOptionEitherHolder(value = Some(Left(value = \"x\")))"
    )

  test("a generic collection element dispatches through the family"):
    assert(
      PrettyPrintable[GenListEitherHolder].describe(GenListEitherHolder(List(Right(1))))(using singleLine) ==
        "GenListEitherHolder(value = [Right(value = 1)])"
    )

  test("a user defined generic sealed trait dispatches to a parameterised child"):
    assert(
      PrettyPrintable[GenResultHolder].describe(GenResultHolder(GenResult.Ok("a")))(using singleLine) ==
        "GenResultHolder(value = Ok(value = \"a\"))"
    )

  test("a user defined generic sealed trait dispatches to a child that ignores the parameter"):
    assert(
      PrettyPrintable[GenResultHolder].describe(GenResultHolder(GenResult.Bad("b")))(using singleLine) ==
        "GenResultHolder(value = Bad(note = \"b\"))"
    )

  test("a generic enum dispatches to a single field case"):
    assert(
      PrettyPrintable[GenPairHolder].describe(GenPairHolder(GenPair.One("a")))(using singleLine) ==
        "GenPairHolder(value = One(value = \"a\"))"
    )

  test("a generic enum dispatches to a multi field case"):
    assert(
      PrettyPrintable[GenPairHolder].describe(GenPairHolder(GenPair.Two("a", "b")))(using singleLine) ==
        "GenPairHolder(value = Two(first = \"a\", second = \"b\"))"
    )

  test("a covariant enum dispatches to a parameterised case"):
    assert(
      PrettyPrintable[GenTreeHolder].describe(GenTreeHolder(GenTree.Leaf("a")))(using singleLine) ==
        "GenTreeHolder(value = Leaf(value = \"a\"))"
    )

  test("a covariant enum dispatches to its singleton case by name"):
    assert(
      PrettyPrintable[GenTreeHolder].describe(GenTreeHolder(GenTree.Empty))(using singleLine) ==
        "GenTreeHolder(value = Empty)"
    )

  test("a two level sealed hierarchy still dispatches through its intermediate trait"):
    assert(
      PrettyPrintable[GenTopHolder].describe(GenTopHolder(GenTop.GenBottom("s3cret", "t")))(using singleLine) ==
        "GenTopHolder(value = GenBottom(s = <redacted>, tag = \"t\"))"
    )

  test("a null generic sealed value renders as null"):
    assert(
      PrettyPrintable[GenEitherHolder].describe(GenEitherHolder(null))(using
        singleLine) == "GenEitherHolder(value = null)"
    )
