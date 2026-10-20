package com.worxbend.reveal

import com.worxbend.reveal.annotations.Excluded
import com.worxbend.reveal.annotations.Redacted

import scala.compiletime.testing.typeCheckErrors

import org.scalatest.funsuite.AnyFunSuite

object ContainerReachabilityFixtures:

  sealed trait Values extends java.lang.Iterable[String]

  final case class SecretValues(@Redacted token: String) extends Values:
    override def iterator(): java.util.Iterator[String] = java.util.Collections.emptyIterator[String]()

  final case class Middle(payload: Values)
  final case class Outer(middle: Middle)
  final case class Direct(payload: Values) derives PrettyPrintable
  final case class DerivedMiddle(payload: Values) derives PrettyPrintable
  final case class DerivedOuter(middle: DerivedMiddle) derives PrettyPrintable

  sealed trait ExcludedValues extends java.lang.Iterable[String]

  final case class ExcludedValue(@Excluded token: String) extends ExcludedValues:
    override def iterator(): java.util.Iterator[String] = java.util.Collections.emptyIterator[String]()

  final case class ExcludedMiddle(payload: ExcludedValues)
  final case class ExcludedOuter(middle: ExcludedMiddle)

  sealed trait ScalaValues extends Iterable[String]

  final case class ScalaSecret(@Redacted token: String) extends ScalaValues:
    override def iterator: Iterator[String] = Iterator.empty

  final case class ScalaMiddle(payload: ScalaValues)
  final case class ScalaOuter(middle: ScalaMiddle)

  sealed trait GenericValues[+A] extends java.lang.Iterable[String]
  sealed trait GenericBranch[+A] extends GenericValues[A]

  final case class GenericValue[A](payload: A) extends GenericBranch[A]:
    override def iterator(): java.util.Iterator[String] = java.util.Collections.emptyIterator[String]()

  case object Empty extends GenericValues[Nothing]:
    override def iterator(): java.util.Iterator[String] = java.util.Collections.emptyIterator[String]()

  final case class Secret(@Redacted token: String)
  final case class GenericSecretMiddle(payload: GenericValues[Secret])
  final case class GenericSecretOuter(middle: GenericSecretMiddle)
  final case class GenericSafeMiddle(payload: GenericValues[String])
  final case class GenericSafeOuter(middle: GenericSafeMiddle) derives PrettyPrintable

  sealed trait AnnotatedValues[A] extends java.lang.Iterable[String]
  sealed trait AnnotatedBranch[A] extends AnnotatedValues[A]

  final case class AnnotatedValue[A](@Redacted token: A) extends AnnotatedBranch[A]:
    override def iterator(): java.util.Iterator[String] = java.util.Collections.emptyIterator[String]()

  final case class AnnotatedMiddle(payload: AnnotatedValues[String])
  final case class AnnotatedOuter(middle: AnnotatedMiddle)

  final class Tagged[A](val tag: A) extends java.util.ArrayList[String]
  final case class TaggedMiddle(payload: Tagged[Secret])
  final case class TaggedOuter(middle: TaggedMiddle)
  final class FixedPayload          extends java.util.ArrayList[Secret]
  final case class FixedMiddle(payload: FixedPayload)
  final case class FixedOuter(middle: FixedMiddle)

  sealed trait IncompleteValues extends java.lang.Iterable[String]
  trait OpenBranch              extends IncompleteValues

  final case class OpenSecret(@Redacted token: String) extends OpenBranch:
    override def iterator(): java.util.Iterator[String] = java.util.Collections.emptyIterator[String]()

  final case class IncompleteMiddle(payload: IncompleteValues)
  final case class IncompleteOuter(middle: IncompleteMiddle)

  sealed trait RecursiveValues extends java.lang.Iterable[String]

  final case class Link(next: Option[RecursiveValues]) extends RecursiveValues:
    override def iterator(): java.util.Iterator[String] = java.util.Collections.emptyIterator[String]()

  final case class RecursiveSecret(@Redacted token: String) extends RecursiveValues:
    override def iterator(): java.util.Iterator[String] = java.util.Collections.emptyIterator[String]()

  final case class RecursiveMiddle(payload: RecursiveValues)
  final case class RecursiveOuter(middle: RecursiveMiddle)

  final case class JavaMiddle(payload: java.lang.Iterable[String])
  final case class JavaOuter(middle: JavaMiddle)
  final case class SeqMiddle(payload: Seq[String])
  final case class SeqOuter(middle: SeqMiddle)
  final case class MapMiddle(payload: scala.collection.Map[String, String])
  final case class MapOuter(middle: MapMiddle)
  final case class DirectJava(payload: java.lang.Iterable[String]) derives PrettyPrintable

  sealed trait UnresolvedValues[A] extends java.lang.Iterable[String]

  final class Extra[A, B](val value: A, val extra: B) extends UnresolvedValues[A]:
    override def iterator(): java.util.Iterator[String] = java.util.Collections.emptyIterator[String]()

  final case class UnresolvedMiddle(payload: UnresolvedValues[String])
  final case class UnresolvedOuter(middle: UnresolvedMiddle)

  type TextVector = Vector[String]
  final case class VectorMiddle(payload: TextVector)
  final case class VectorOuter(middle: VectorMiddle) derives PrettyPrintable
  final case class VectorSecretMiddle(payload: Vector[Secret])
  final case class VectorSecretOuter(middle: VectorSecretMiddle)
  final case class VectorUnknownMiddle(payload: Vector[Any])
  final case class VectorUnknownOuter(middle: VectorUnknownMiddle)

final class ContainerReachabilitySuite extends AnyFunSuite:

  import ContainerReachabilityFixtures.*

  test("sealed container children cannot hide annotated state outside the element type"):
    val errors = typeCheckErrors("PrettyPrintable.derived[Outer]").map(_.message)
    assert(
      errors.exists(message =>
        message.contains("will not render") && message.contains("SecretValues") && message.contains("`token`")
      ),
      errors.mkString("\n"),
    )

  test("sealed container children cannot hide excluded state"):
    val errors = typeCheckErrors("PrettyPrintable.derived[ExcludedOuter]").map(_.message)
    assert(
      errors.exists(message =>
        message.contains("will not render") && message.contains("ExcludedValue") && message.contains("`token`")
      ),
      errors.mkString("\n"),
    )

  test("Scala Iterable subtypes also expose their sealed children to the scan"):
    val errors = typeCheckErrors("PrettyPrintable.derived[ScalaOuter]").map(_.message)
    assert(
      errors.exists(message => message.contains("ScalaSecret") && message.contains("`token`")),
      errors.mkString("\n"),
    )

  test("generic container arguments remain subject to the annotation scan"):
    val errors = typeCheckErrors("PrettyPrintable.derived[GenericSecretOuter]").map(_.message)
    assert(errors.exists(message => message.contains("Secret") && message.contains("`token`")), errors.mkString("\n"))

  test("multi-level generic container children retain their applied types with safe parent arguments"):
    val errors = typeCheckErrors("PrettyPrintable.derived[AnnotatedOuter]").map(_.message)
    assert(
      errors.exists(message => message.contains("AnnotatedValue[java.lang.String]") && message.contains("`token`")),
      errors.mkString("\n"),
    )

  test("a descendant instance cannot protect an enclosing ordinary toString"):
    given PrettyPrintable[SecretValues] = PrettyPrintable.derived[SecretValues]
    val errors                          = typeCheckErrors("PrettyPrintable.derived[Outer]").map(_.message)
    assert(errors.exists(_.contains("`token`")), errors.mkString("\n"))

  test("container subtypes retain independent declared type arguments"):
    val errors = typeCheckErrors("PrettyPrintable.derived[TaggedOuter]").map(_.message)
    assert(errors.exists(_.contains("`token`")), errors.mkString("\n"))

  test("concrete container subtypes retain inherited element types"):
    val errors = typeCheckErrors("PrettyPrintable.derived[FixedOuter]").map(_.message)
    assert(errors.exists(_.contains("`token`")), errors.mkString("\n"))

  test("a sealed container family with an open branch remains unresolved"):
    val errors = typeCheckErrors("PrettyPrintable.derived[IncompleteOuter]").map(_.message)
    assert(
      errors.exists(message => message.contains("cannot prove") && message.contains("OpenBranch")),
      errors.mkString("\n"),
    )

  test("open container interfaces behind ordinary toString remain unresolved"):
    val errors = List(
      typeCheckErrors("PrettyPrintable.derived[JavaOuter]"),
      typeCheckErrors("PrettyPrintable.derived[SeqOuter]"),
      typeCheckErrors("PrettyPrintable.derived[MapOuter]"),
    )
    errors.foreach(found => assert(found.exists(_.message.contains("cannot prove")), found.mkString("\n")))

  test("a completed recursive child scan cannot hide an annotated sibling branch"):
    val errors = typeCheckErrors("PrettyPrintable.derived[RecursiveOuter]").map(_.message)
    assert(
      errors.exists(message => message.contains("RecursiveSecret") && message.contains("`token`")),
      errors.mkString("\n"),
    )

  test("an unresolvable child recommends an instance at the enclosing fallback boundary"):
    val errors = typeCheckErrors("PrettyPrintable.derived[UnresolvedOuter]").map(_.message)
    assert(
      errors.exists(message => message.contains("cannot prove") && message.contains("Extra")),
      errors.mkString("\n"),
    )
    assert(
      errors.exists(_.contains(
        "given PrettyPrintable[com.worxbend.reveal.ContainerReachabilityFixtures.UnresolvedMiddle]"
      )),
      errors.mkString("\n"),
    )

  test("an instance below the fallback boundary cannot resolve an unknown child"):
    given PrettyPrintable[UnresolvedValues[String]] with
      override def describe(value: UnresolvedValues[String])(using Configuration): String = "HIDDEN"
    val errors = typeCheckErrors("PrettyPrintable.derived[UnresolvedOuter]").map(_.message)
    assert(
      errors.exists(message => message.contains("cannot prove") && message.contains("Extra")),
      errors.mkString("\n"),
    )

  test("the suggested instance at the fallback boundary handles an unresolvable child"):
    given PrettyPrintable[UnresolvedMiddle] with
      override def describe(value: UnresolvedMiddle)(using Configuration): String = "HIDDEN"
    val instance = PrettyPrintable.derived[UnresolvedOuter]
    val value    = UnresolvedOuter(UnresolvedMiddle(new Extra("safe", Secret("TOKEN_MARKER"))))
    assert(instance.describe(value)(using Configuration.default) == "UnresolvedOuter(middle = HIDDEN)")

  test("safe generic container families retain ordinary toString fallback"):
    val value = GenericSafeOuter(GenericSafeMiddle(GenericValue("safe")))
    assert(value.asString == "GenericSafeOuter(middle = GenericSafeMiddle(GenericValue(safe)))")

  test("singleton container branches retain ordinary toString fallback"):
    val value = GenericSafeOuter(GenericSafeMiddle(Empty))
    assert(value.asString == "GenericSafeOuter(middle = GenericSafeMiddle(Empty))")

  test("the standard Vector and its aliases retain ordinary toString fallback"):
    assert(VectorOuter(VectorMiddle(Vector("safe"))).asString == "VectorOuter(middle = VectorMiddle(Vector(safe)))")

  test("the standard Vector still scans annotated element types"):
    val errors = typeCheckErrors("PrettyPrintable.derived[VectorSecretOuter]").map(_.message)
    assert(errors.exists(_.contains("`token`")), errors.mkString("\n"))

  test("the standard Vector still refuses unresolved element types"):
    val errors = typeCheckErrors("PrettyPrintable.derived[VectorUnknownOuter]").map(_.message)
    assert(
      errors.exists(message => message.contains("cannot prove") && message.contains("scala.Any")),
      errors.mkString("\n"),
    )

  test("direct structural rendering does not print unrelated container fields"):
    assert(Direct(SecretValues("TOKEN_MARKER")).asString == "Direct(payload = [])")

  test("direct structural rendering still accepts open container interfaces"):
    assert(DirectJava(SecretValues("TOKEN_MARKER")).asString == "DirectJava(payload = [])")

  test("deriving the intermediate renders its container structurally"):
    assert(DerivedOuter(DerivedMiddle(SecretValues("TOKEN_MARKER"))).asString ==
      "DerivedOuter(middle = DerivedMiddle(payload = []))")
