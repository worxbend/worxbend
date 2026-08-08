package com.worxbend.dscrbo

import com.worxbend.dscrbo.annotations.Excluded
import com.worxbend.dscrbo.annotations.Redacted

import scala.compiletime.testing.typeCheckErrors

import org.scalatest.funsuite.AnyFunSuite

// A nested case class that carries its own instance: rendered structurally, through that instance.
final case class DelInstanced(a: Int, s: String) derives Describe

// The same shape without one: rendered by its own toString, not unrolled into whoever holds it.
final case class DelPlain(a: Int, s: String)

// Without an instance but carrying redaction, which is the one combination that is refused rather than delegated.
final case class DelSecret(@Redacted token: String, tag: String)

final case class DelExcluding(@Excluded hidden: String, tag: String)

final case class DelHoldsInstanced(inner: DelInstanced) derives Describe
final case class DelHoldsPlain(inner: DelPlain) derives Describe
final case class DelHoldsPlainList(inner: List[DelPlain]) derives Describe
final case class DelHoldsPlainOption(inner: Option[DelPlain]) derives Describe

final case class DelId(raw: Long) extends AnyVal
final case class DelHoldsValueClass(id: DelId) derives Describe

enum DelColour derives Describe:

  case Red
  case Sized(n: Int, label: String)

final case class DelHoldsEnum(c: DelColour) derives Describe

/** The delegation contract: what this macro expands, and what it hands to `toString`.
  *
  * A nested case class is not unrolled into its parent. It earns structured rendering by carrying its own instance,
  * exactly as any other typeclass would require, and without one it renders the way Scala already renders it. Enums,
  * sealed families and value classes keep their structural treatment, because for those there is no instance to
  * delegate to and inlining is the whole point.
  */
final class DelegationSuite extends AnyFunSuite:

  private given Configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = -1)

  private def show[A](a: A)(using d: Describe[A]): String = d.describe(a)

  test("a nested case class with its own instance renders structurally"):
    assert(show(DelHoldsInstanced(DelInstanced(
      1,
      "v",
    ))) == """DelHoldsInstanced(inner = DelInstanced(a = 1, s = "v"))""")

  test("a nested case class without an instance renders with its own toString"):
    assert(show(DelHoldsPlain(DelPlain(1, "v"))) == "DelHoldsPlain(inner = DelPlain(1,v))")

  test("the same holds for a nested case class inside a collection"):
    assert(show(DelHoldsPlainList(List(DelPlain(1, "v")))) == "DelHoldsPlainList(inner = [DelPlain(1,v)])")

  test("the same holds for a nested case class inside an Option"):
    assert(show(DelHoldsPlainOption(Some(DelPlain(1, "v")))) == "DelHoldsPlainOption(inner = Some(DelPlain(1,v)))")

  test("giving the nested type an instance is all it takes to get structure back"):
    given Describe[DelPlain] = Describe.derived[DelPlain]
    assert(Describe.derived[DelHoldsPlain].describe(DelHoldsPlain(DelPlain(
      1,
      "v",
    ))) == """DelHoldsPlain(inner = DelPlain(a = 1, s = "v"))""")

  // The one case where delegating to toString would defeat the library's purpose, so it is a compile error instead.
  test("a nested case class with @Redacted and no instance is refused"):
    val errors = typeCheckErrors("Describe.derived[DelHoldsSecret]").map(_.message)
    assert(errors.nonEmpty)

  test("the refusal names the offending field and prescribes a remedy"):
    val errors = typeCheckErrors("""
      final case class Holder(s: DelSecret) derives Describe
    """).map(_.message)
    assert(errors.exists(_.contains("`token`")), errors.mkString("\n"))
    assert(errors.exists(_.contains("derives Describe")), errors.mkString("\n"))

  test("@Excluded on a nested type without an instance is refused for the same reason"):
    val errors = typeCheckErrors("""
      final case class Holder(s: DelExcluding) derives Describe
    """).map(_.message)
    assert(errors.exists(_.contains("`hidden`")), errors.mkString("\n"))

  test("giving the redacting type an instance makes the same shape compile, and it redacts"):
    given Describe[DelSecret] = Describe.derived[DelSecret]
    final case class Holder(s: DelSecret) derives Describe
    assert(Describe.derived[Holder].describe(Holder(DelSecret("hunter2", "t"))).contains("<redacted>"))

  // ------------------- the guard reaches all the way down, not one level

  test("an unannotated intermediate hiding a redacted descendant is refused"):
    val errors = typeCheckErrors("final case class H(m: DelDeepMiddle) derives Describe").map(_.message)
    assert(errors.nonEmpty, "a redacted descendant must not reach toString")
    assert(errors.exists(_.contains("DelDeepSecret")), errors.mkString("\n"))
    assert(errors.exists(_.contains("`token`")), errors.mkString("\n"))

  test("a redacted descendant reached through a collection is refused too"):
    val errors = typeCheckErrors("final case class H(m: DelDeepListMiddle) derives Describe").map(_.message)
    assert(errors.nonEmpty, "a List[Secret] behind an intermediate must not reach toString")

  test("an excluded descendant behind an intermediate is refused on the same grounds"):
    val errors = typeCheckErrors("final case class H(m: DelDeepExcludingMiddle) derives Describe").map(_.message)
    assert(errors.exists(_.contains("`hidden`")), errors.mkString("\n"))

  test("a redacted branch of a sealed family behind an intermediate is refused"):
    val errors = typeCheckErrors("final case class H(m: DelSealedMiddle) derives Describe").map(_.message)
    assert(errors.nonEmpty, "a redacting sealed branch must not reach toString")

  test("giving the intermediate an instance makes it compile, and the descendant is redacted"):
    given Describe[DelDeepSecret] = Describe.derived[DelDeepSecret]
    given Describe[DelDeepMiddle] = Describe.derived[DelDeepMiddle]
    final case class Holder(m: DelDeepMiddle) derives Describe
    assert(Describe.derived[Holder].describe(Holder(DelDeepMiddle(DelDeepSecret("hunter2")))).contains("<redacted>"))

  test("a type with nothing annotated anywhere below it still delegates"):
    assert(show(DelHoldsPlain(DelPlain(1, "v"))) == "DelHoldsPlain(inner = DelPlain(1,v))")

  // ------------------------------- shapes that still expand structurally

  test("a value class is still seen through, with no instance of its own"):
    assert(show(DelHoldsValueClass(DelId(7L))) == "DelHoldsValueClass(id = 7)")

  test("an enum singleton case is still rendered by name"):
    assert(show(DelHoldsEnum(DelColour.Red)) == "DelHoldsEnum(c = Red)")

  test("an enum case with fields is still expanded structurally"):
    assert(show(DelHoldsEnum(DelColour.Sized(2, "m"))) == """DelHoldsEnum(c = Sized(n = 2, label = "m"))""")

final case class DelHoldsSecret(s: DelSecret)

// An unannotated intermediate does not make the descendant safe: toString ignores the annotations at every depth.
final case class DelDeepSecret(@Redacted token: String)
final case class DelDeepMiddle(s: DelDeepSecret)
final case class DelDeepListMiddle(xs: List[DelDeepSecret])
final case class DelDeepExcludingMiddle(s: DelExcluding)

sealed trait DelSealedSecret
final case class DelSealedBranch(@Redacted token: String) extends DelSealedSecret
final case class DelSealedMiddle(s: DelSealedSecret)
