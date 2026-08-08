package com.worxbend.describo

import com.worxbend.describo.annotations.Excluded
import com.worxbend.describo.annotations.Redacted
import com.worxbend.tck.TckAdapter
import com.worxbend.tck.TckConfiguration
import com.worxbend.tck.TckConformance

/** Fixtures for the shared conformance kit, rendered through `Printable`.
  *
  * The values live here rather than in the kit because each renderer reaches its instances differently — this module
  * needs `derives Printable`, its sibling needs `derives Describe` — so only the *expectations* can be shared.
  */
private object TckFixtures:

  final case class Primitives(c: Char, by: Byte, sh: Short, i: Int, l: Long, f: Float, d: Double, bool: Boolean)
      derives Printable

  final case class BigNumbers(dec: BigDecimal, int: BigInt) derives Printable
  final case class StringHolder(s: String) derives Printable
  final case class CharHolder(c: Char) derives Printable

  final case class Listish(xs: List[String]) derives Printable
  final case class Setish(xs: Set[Int]) derives Printable
  final case class Vectorish(xs: Vector[Int]) derives Printable
  final case class Arrayish(xs: Array[Int]) derives Printable
  final case class Nestedish(xs: List[List[String]]) derives Printable
  final case class Mapish(m: Map[String, String]) derives Printable
  final case class MapIntish(m: Map[Int, String]) derives Printable
  final case class Optionish(o: Option[String]) derives Printable
  final case class JavaListish(xs: java.util.List[String]) derives Printable
  final case class JavaSetish(xs: java.util.Set[String]) derives Printable
  final case class JavaMapish(m: java.util.Map[String, String]) derives Printable

  final case class Empty() derives Printable
  final case class Single(only: Int) derives Printable
  final case class Inner(v: Int) derives Printable
  final case class Outer(inner: Inner, tag: String) derives Printable
  final case class ValueHolder(id: TckIdent) derives Printable
  case object TheObject
  final case class CaseObjectHolder(o: TheObject.type) derives Printable
  final case class SealedHolder(s: Base) derives Printable
  final case class EnumHolder(e: Colour) derives Printable
  final case class EnumFieldsHolder(e: Colour) derives Printable
  final case class EitherHolder(e: Either[String, Int]) derives Printable
  final case class Pair(a: Int, b: Int) derives Printable
  final case class TupleHolder(pair: (Int, String)) derives Printable
  final case class TupleNestedHolder(pair: (Inner, String)) derives Printable
  final case class Node(v: Int, next: Option[Node]) derives Printable

  final case class Secretish(@Redacted secret: String, tag: String) derives Printable
  final case class SecretCustom(@Redacted(replacement = "***") secret: String, tag: String) derives Printable
  final case class Excludish(@Excluded hidden: String, tag: String) derives Printable
  final case class Transientish(@transient hidden: String, tag: String) derives Printable
  final case class BothAnnotations(@Redacted @Excluded both: String, tag: String) derives Printable
  final case class BothAnnotationsReversed(@Excluded @Redacted both: String, tag: String) derives Printable

  final case class RepeatedRedaction(
      @Redacted(replacement = "first") @Redacted(replacement = "second") secret: String,
      tag:                                                                       String,
  ) derives Printable

  final case class RedactedInner(@Redacted secret: String) derives Printable
  final case class RedactedOuter(inner: RedactedInner, tag: String) derives Printable
  final case class RedactedCollection(xs: List[RedactedInner]) derives Printable
  final case class RedactedOption(o: Option[RedactedInner]) derives Printable
  final case class RedactedMap(m: Map[String, RedactedInner]) derives Printable
  final case class AllExcluded(@Excluded a: String) derives Printable

/** A value class has no `Mirror`, so magnolia cannot auto-derive it and it needs the explicit factory. That is a
  * difference in how the *instance* is obtained, not in what it renders — which is precisely the kind of asymmetry the
  * shared catalogue is meant to tolerate while still pinning the output.
  */
final case class TckIdent(v: Int) extends AnyVal

object TckIdent:

  given Printable[TckIdent] =
    Printable.valueClass[TckIdent, Int]("TckIdent", "com.worxbend.describo.TckIdent")(_.v)

/** Top-level on purpose: the qualified-name obligations expect `<package>.TckQualified`, so this must not be
  * nested inside the fixtures object.
  */
final case class TckQualified(a: Int, s: String) derives Printable

sealed trait Base derives Printable
final case class Child(v: Int) extends Base

enum Colour derives Printable:

  case Red
  case Sized(n: Int)

private object DescriboTckAdapter extends TckAdapter:

  import TckFixtures.*

  override def rendererName: String = "describo"

  override def fixturePackage: String = "com.worxbend.describo"

  /** One obvious line per field: a compile error here is the signal that the kit's record and this module's
    * `Configuration` have drifted apart.
    */
  private def configurationOf(c: TckConfiguration): Configuration =
    Configuration(
      useFieldNames = c.useFieldNames,
      useTypeNames = c.useTypeNames,
      fullyQualifiedClassName = c.fullyQualifiedClassName,
      shortPackagePrefix = c.shortPackagePrefix,
      fieldsSeparator = c.fieldsSeparator,
      fieldNamePrefix = c.fieldNamePrefix,
      fieldNameSuffix = c.fieldNameSuffix,
      fieldNameAndValueSeparator = c.fieldNameAndValueSeparator,
      fieldNameAndTypeNameSeparator = c.fieldNameAndTypeNameSeparator,
      typeNamePrefix = c.typeNamePrefix,
      typeNameSuffix = c.typeNameSuffix,
      valuePrefix = c.valuePrefix,
      valueSuffix = c.valueSuffix,
      multiline = c.multiline,
      multilineIndent = c.multilineIndent,
      multilineIfFieldsAreGreaterOrEqual = c.multilineIfFieldsAreGreaterOrEqual,
    )

  override def render(fixtureId: String, configuration: TckConfiguration): String =
    given Configuration = configurationOf(configuration)

    def show[A](a: A)(using p: Printable[A]): String = p.asString(a)

    fixtureId match
      case "primitives"            => show(Primitives('x', 1, 2, 3, 4L, 5.5f, 6.5d, true))
      case "big-numbers"           => show(BigNumbers(BigDecimal("1000.50"), BigInt("99999999999999999999")))
      case "string-plain"          => show(StringHolder("hello"))
      case "string-empty"          => show(StringHolder(""))
      case "string-null"           => show(StringHolder(null))
      case "string-separators"     => show(StringHolder("a,b)c(d"))
      case "string-escapes"        => show(StringHolder("q\"b\\s\nn\tt\rr"))
      case "char-quote"            => show(CharHolder('\''))
      case "list-strings"          => show(Listish(List("a", "b")))
      case "list-empty"            => show(Listish(Nil))
      case "list-null"             => show(Listish(null))
      case "set-ints"              => show(Setish(Set(1)))
      case "vector-ints"           => show(Vectorish(Vector(1, 2)))
      case "array-ints"            => show(Arrayish(Array(1, 2)))
      case "array-null"            => show(Arrayish(null))
      case "nested-collections"    => show(Nestedish(List(List("a"), List("b"))))
      case "map-strings"           => show(Mapish(Map("k" -> "v")))
      case "map-int-keys"          => show(MapIntish(Map(1 -> "v")))
      case "map-empty"             => show(Mapish(Map.empty))
      case "map-null"              => show(Mapish(null))
      case "option-some"           => show(Optionish(Some("v")))
      case "option-none"           => show(Optionish(None))
      case "option-null"           => show(Optionish(null))
      case "java-list"             => show(JavaListish(java.util.List.of("a")))
      case "java-set"              => show(JavaSetish(java.util.Set.of("a")))
      case "java-map"              => show(JavaMapish(java.util.Map.of("k", "v")))
      case "empty-case-class"      => show(Empty())
      case "single-field"          => show(Single(1))
      case "nested"                => show(Outer(Inner(1), "t"))
      case "nested-null"           => show(Outer(null, "t"))
      case "value-class"           => show(ValueHolder(TckIdent(7)))
      case "case-object"           => show(CaseObjectHolder(TheObject))
      case "sealed-child"          => show(SealedHolder(Child(1)))
      case "enum-singleton"        => show(EnumHolder(Colour.Red))
      case "enum-fields"           => show(EnumFieldsHolder(Colour.Sized(2)))
      case "either-right"          => show(EitherHolder(Right(1)))
      case "either-left"           => show(EitherHolder(Left("e")))
      case "tuple-scalars"         => show(TupleHolder((1, "a")))
      case "tuple-nested"          => show(TupleNestedHolder((Inner(1), "t")))
      case "qualified-names"       => show(TckQualified(1, "v"))
      case "deep-recursion"        => show(Node(1, Some(Node(2, Some(Node(3, Some(Node(4, None))))))))
      case "layout-threshold"      => show(Pair(1, 2))
      case "redacted-default"      => show(Secretish("s", "t"))
      case "redacted-custom"       => show(SecretCustom("s", "t"))
      case "redacted-null"         => show(Secretish(null, "t"))
      case "excluded"              => show(Excludish("h", "t"))
      case "transient-field"       => show(Transientish("h", "t"))
      case "redacted-and-excluded" => show(BothAnnotations("s", "t"))
      case "excluded-and-redacted" => show(BothAnnotationsReversed("s", "t"))
      case "repeated-redacted"     => show(RepeatedRedaction("s", "t"))
      case "nested-redaction"      => show(RedactedOuter(RedactedInner("s"), "t"))
      case "collection-redaction"  => show(RedactedCollection(List(RedactedInner("s"))))
      case "option-redaction"      => show(RedactedOption(Some(RedactedInner("s"))))
      case "map-value-redaction"   => show(RedactedMap(Map("k" -> RedactedInner("s"))))
      case "all-excluded"          => show(AllExcluded("a"))
      case other                   => throw TckAdapter.UnknownFixture(other)

/** describo's half of the shared parity contract. */
class TckConformanceSuite extends TckConformance(DescriboTckAdapter)
