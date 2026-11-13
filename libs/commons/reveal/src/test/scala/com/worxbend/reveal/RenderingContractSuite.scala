package com.worxbend.reveal

import com.worxbend.reveal.annotations.Redacted

import scala.compiletime.testing.typeCheckErrors

import org.scalatest.funsuite.AnyFunSuite

object RenderingContractFixtures:

  final case class Secret(@Redacted token: String) derives PrettyPrintable
  final case class Untyped(payload: Any)
  final case class UntypedOuter(inner: Untyped)
  trait Open
  final case class OpenSecret(@Redacted token: String) extends Open
  final case class OpenMiddle(payload: Open)
  final case class OpenOuter(inner: OpenMiddle)
  final case class UnknownElements(payload: List[Any])
  final case class UnknownElementsOuter(inner: UnknownElements)
  final case class UnionMiddle(payload: Open | String)
  final case class UnionOuter(inner: UnionMiddle)
  final case class WildcardMiddle(payload: List[?])
  final case class WildcardOuter(inner: WildcardMiddle)
  sealed trait IncompleteFamily
  trait OpenBranch                                     extends IncompleteFamily
  final case class IncompleteMiddle(payload: IncompleteFamily)
  final case class IncompleteOuter(inner: IncompleteMiddle)
  final case class SafeMiddle(payload: List[Option[String]])
  final case class SafeOuter(inner: SafeMiddle) derives PrettyPrintable
  sealed trait SafeFamily[A]
  final case class SafeBranch[A](payload: A)           extends SafeFamily[A]
  final case class SafeFamilyMiddle(payload: SafeFamily[String])
  final case class SafeFamilyOuter(inner: SafeFamilyMiddle) derives PrettyPrintable
  final case class SecretFamilyMiddle(payload: SafeFamily[Secret])
  final case class SecretFamilyOuter(inner: SecretFamilyMiddle)

  case object Singleton
  sealed trait Event

  case object Stop extends Event:

    given PrettyPrintable[Stop.type] with

      override def describe(value: Stop.type)(using conf: Configuration): String =
        conf.valuePrefix + "CUSTOM-STOP" + conf.valueSuffix

  case object Go extends Event
  final case class Events(direct: Stop.type, widened: Event) derives PrettyPrintable

  type Ten[A] = Option[Option[Option[Option[Option[Option[Option[Option[Option[Option[A]]]]]]]]]]
  final case class Twenty(payload: Ten[Ten[String]])
  final case class TwentyOne(payload: Ten[Ten[Option[String]]])
  final case class DeepList(payload: Ten[Ten[List[String]]])
  final case class DeepMap(payload: Ten[Ten[Map[String, Int]]])
  final case class DeepArray(payload: Ten[Ten[Array[String]]])
  final case class DeepJavaList(payload: Ten[Ten[java.util.List[String]]])
  final case class DeepJavaMap(payload: Ten[Ten[java.util.Map[String, Int]]])

final class RenderingContractSuite extends AnyFunSuite:

  import RenderingContractFixtures.*

  test("ordinary toString cannot hide an unresolved Any descendant"):
    val errors = typeCheckErrors("PrettyPrintable.derived[UntypedOuter]")
    assert(errors.exists(_.message.contains("cannot prove")), errors.mkString("\n"))

  test("ordinary toString cannot hide an open trait descendant"):
    val errors = typeCheckErrors("PrettyPrintable.derived[OpenOuter]")
    assert(errors.exists(_.message.contains("Open")), errors.mkString("\n"))
    assert(errors.exists(_.message.contains("cannot prove")), errors.mkString("\n"))

  test("container arguments remain subject to the redaction reachability scan"):
    val errors = typeCheckErrors("PrettyPrintable.derived[UnknownElementsOuter]")
    assert(errors.exists(_.message.contains("cannot prove")), errors.mkString("\n"))

  test("unions behind an ordinary product remain unresolved"):
    val errors = typeCheckErrors("PrettyPrintable.derived[UnionOuter]")
    assert(errors.exists(_.message.contains("cannot prove")), errors.mkString("\n"))

  test("wildcard container payloads cannot bypass the fallback scan"):
    val errors = typeCheckErrors("PrettyPrintable.derived[WildcardOuter]")
    assert(errors.exists(_.message.contains("cannot prove")), errors.mkString("\n"))

  test("a sealed family with an open branch remains unresolved"):
    val errors = typeCheckErrors("PrettyPrintable.derived[IncompleteOuter]")
    assert(errors.exists(_.message.contains("cannot prove")), errors.mkString("\n"))

  test("a generic sealed payload instance does not protect an enclosing ordinary toString"):
    val errors = typeCheckErrors("PrettyPrintable.derived[SecretFamilyOuter]")
    assert(errors.exists(_.message.contains("token")), errors.mkString("\n"))

  test("known containers with concrete payloads retain ordinary toString fallback"):
    assert(SafeOuter(SafeMiddle(List(Some("safe")))).asString ==
      "SafeOuter(inner = SafeMiddle(List(Some(safe))))")

  test("generic sealed descendants use their substituted payload type"):
    assert(SafeFamilyOuter(SafeFamilyMiddle(SafeBranch("safe"))).asString ==
      "SafeFamilyOuter(inner = SafeFamilyMiddle(SafeBranch(safe)))")

  test("a supplied instance at the fallback boundary can safely handle an unknown payload"):
    given PrettyPrintable[Untyped] with
      override def describe(value: Untyped)(using Configuration): String = "HIDDEN"
    assert(PrettyPrintable.derived[UntypedOuter].describe(UntypedOuter(Untyped(Secret("marker"))))(using
      Configuration.default) == "UntypedOuter(inner = HIDDEN)")

  test("the inline singleton value expression is evaluated exactly once"):
    var evaluations            = 0
    def next(): Singleton.type =
      evaluations += 1
      Singleton
    val rendered               = ToString.derived(next())
    assert(rendered == "Singleton")
    assert(evaluations == 1)

  test("a singleton producer exception propagates unchanged"):
    val failure                = IllegalStateException("producer failed")
    def next(): Singleton.type = throw failure
    assert(intercept[IllegalStateException](ToString.derived(next())) eq failure)

  test("inline configuration evaluates once before the singleton producer"):
    val events                         = scala.collection.mutable.ArrayBuffer.empty[String]
    def configuration(): Configuration =
      events += "configuration"
      Configuration.default
    def next(): Singleton.type         =
      events += "value"
      Singleton
    val rendered                       = ToString.derived(next())(using configuration())
    assert(rendered == "Singleton")
    assert(events.toList == List("configuration", "value"))

  test("null singleton roots and fields render null"):
    final case class Holder(singleton: Singleton.type) derives PrettyPrintable
    assert(ToString.derived(null.asInstanceOf[Singleton.type]) == "null")
    val absent = null.asInstanceOf[Singleton.type]
    assert(PrettyPrintable.derived[Singleton.type].describe(absent)(using Configuration.default) == "null")
    assert(Holder(absent).asString == "Holder(singleton = null)")

  test("singleton customization survives widening to a sealed parent"):
    given Configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = -1)
    assert(Events(Stop, Stop).asString == "Events(direct = CUSTOM-STOP, widened = CUSTOM-STOP)")

  test("singleton dispatch passes the caller configuration to the custom instance"):
    given Configuration = Configuration(valuePrefix = "<", valueSuffix = ">", multilineIfFieldsAreGreaterOrEqual = -1)
    assert(Events(Stop, Stop).asString == "Events(direct = <<CUSTOM-STOP>>, widened = <<CUSTOM-STOP>>)")

  test("singleton dispatch keeps the default name when no custom instance exists"):
    assert(PrettyPrintable.derived[Event].describe(Go)(using Configuration.default) == "Go")

  test("twenty wrapper layers ending at a scalar compile"):
    assertCompiles("PrettyPrintable.derived[Twenty]")

  test("twenty one wrapper layers ending at a scalar report the layer budget"):
    val errors = typeCheckErrors("PrettyPrintable.derived[TwentyOne]")
    assert(errors.exists(_.message.contains("20 layers of generated code")), errors.mkString("\n"))

  test("each container handler enforces the emitted layer budget"):
    val errors = List(
      typeCheckErrors("PrettyPrintable.derived[DeepList]"),
      typeCheckErrors("PrettyPrintable.derived[DeepMap]"),
      typeCheckErrors("PrettyPrintable.derived[DeepArray]"),
      typeCheckErrors("PrettyPrintable.derived[DeepJavaList]"),
      typeCheckErrors("PrettyPrintable.derived[DeepJavaMap]"),
    )
    errors.foreach(found =>
      assert(found.exists(_.message.contains("20 layers of generated code")), found.mkString("\n"))
    )

  test("delegation breaks a wrapper chain before the layer budget is charged"):
    given PrettyPrintable[Ten[String]] with
      override def describe(value: Ten[String])(using Configuration): String = "TEN"
    assertCompiles("PrettyPrintable.derived[TwentyOne]")
