package com.worxbend.reveal

import com.worxbend.reveal.annotations.Excluded
import com.worxbend.reveal.annotations.Redacted

import scala.compiletime.testing.typeCheckErrors

import org.scalatest.funsuite.AnyFunSuite

// A nested case class that carries its own instance: rendered structurally, through that instance.
final case class DelInstanced(a: Int, s: String) derives PrettyPrintable

// The same shape without one: rendered by its own toString, not unrolled into whoever holds it.
final case class DelPlain(a: Int, s: String)

// Without an instance but carrying redaction, which is the one combination that is refused rather than delegated.
final case class DelSecret(@Redacted token: String, tag: String)

final case class DelExcluding(@Excluded hidden: String, tag: String)

final case class DelHoldsInstanced(inner: DelInstanced) derives PrettyPrintable
final case class DelHoldsPlain(inner: DelPlain) derives PrettyPrintable
final case class DelHoldsPlainList(inner: List[DelPlain]) derives PrettyPrintable
final case class DelHoldsPlainOption(inner: Option[DelPlain]) derives PrettyPrintable

final case class DelId(raw: Long) extends AnyVal
final case class DelHoldsValueClass(id: DelId) derives PrettyPrintable

enum DelColour derives PrettyPrintable:

  case Red
  case Sized(n: Int, label: String)

final case class DelHoldsEnum(c: DelColour) derives PrettyPrintable

/** The delegation contract: what this macro expands, and what it hands to `toString`.
  *
  * A nested case class is not unrolled into its parent. It earns structured rendering by carrying its own instance,
  * exactly as any other typeclass would require, and without one it renders the way Scala already renders it. Enums,
  * sealed families and value classes keep their structural treatment, because for those there is no instance to
  * delegate to and inlining is the whole point.
  */
final class DelegationSuite extends AnyFunSuite:

  private given Configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = -1)

  private def show[A](a: A)(using d: PrettyPrintable[A]): String = d.describe(a)

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
    given PrettyPrintable[DelPlain] = PrettyPrintable.derived[DelPlain]
    assert(PrettyPrintable.derived[DelHoldsPlain].describe(DelHoldsPlain(DelPlain(
      1,
      "v",
    ))) == """DelHoldsPlain(inner = DelPlain(a = 1, s = "v"))""")

  // The one case where delegating to toString would defeat the library's purpose, so it is a compile error instead.
  test("a nested case class with @Redacted and no instance is refused"):
    val errors = typeCheckErrors("PrettyPrintable.derived[DelHoldsSecret]").map(_.message)
    assert(errors.nonEmpty)

  test("the refusal names the offending field and prescribes a remedy"):
    val errors = typeCheckErrors("""
      final case class Holder(s: DelSecret) derives PrettyPrintable
    """).map(_.message)
    assert(errors.exists(_.contains("`token`")), errors.mkString("\n"))
    assert(errors.exists(_.contains("derives PrettyPrintable")), errors.mkString("\n"))

  test("@Excluded on a nested type without an instance is refused for the same reason"):
    val errors = typeCheckErrors("""
      final case class Holder(s: DelExcluding) derives PrettyPrintable
    """).map(_.message)
    assert(errors.exists(_.contains("`hidden`")), errors.mkString("\n"))

  test("giving the redacting type an instance makes the same shape compile, and it redacts"):
    given PrettyPrintable[DelSecret] = PrettyPrintable.derived[DelSecret]
    final case class Holder(s: DelSecret) derives PrettyPrintable
    assert(PrettyPrintable.derived[Holder].describe(Holder(DelSecret("hunter2", "t"))).contains("<redacted>"))

  // ------------------- the guard reaches all the way down, not one level

  test("an unannotated intermediate hiding a redacted descendant is refused"):
    val errors = typeCheckErrors("final case class H(m: DelDeepMiddle) derives PrettyPrintable").map(_.message)
    assert(errors.nonEmpty, "a redacted descendant must not reach toString")
    assert(errors.exists(_.contains("DelDeepSecret")), errors.mkString("\n"))
    assert(errors.exists(_.contains("`token`")), errors.mkString("\n"))

  test("a redacted descendant reached through a collection is refused too"):
    val errors = typeCheckErrors("final case class H(m: DelDeepListMiddle) derives PrettyPrintable").map(_.message)
    assert(errors.nonEmpty, "a List[Secret] behind an intermediate must not reach toString")

  test("an excluded descendant behind an intermediate is refused on the same grounds"):
    val errors = typeCheckErrors("final case class H(m: DelDeepExcludingMiddle) derives PrettyPrintable").map(_.message)
    assert(errors.exists(_.contains("`hidden`")), errors.mkString("\n"))

  test("a redacted branch of a sealed family behind an intermediate is refused"):
    val errors = typeCheckErrors("final case class H(m: DelSealedMiddle) derives PrettyPrintable").map(_.message)
    assert(errors.nonEmpty, "a redacting sealed branch must not reach toString")

  test("giving the intermediate an instance makes it compile, and the descendant is redacted"):
    given PrettyPrintable[DelDeepSecret] = PrettyPrintable.derived[DelDeepSecret]
    given PrettyPrintable[DelDeepMiddle] = PrettyPrintable.derived[DelDeepMiddle]
    final case class Holder(m: DelDeepMiddle) derives PrettyPrintable
    assert(
      PrettyPrintable.derived[Holder].describe(Holder(DelDeepMiddle(DelDeepSecret("hunter2")))).contains("<redacted>")
    )

  test("a type with nothing annotated anywhere below it still delegates"):
    assert(show(DelHoldsPlain(DelPlain(1, "v"))) == "DelHoldsPlain(inner = DelPlain(1,v))")

  // Regression: the scan cuts cycles by remembering types already on the path, which is enough only when the type
  // graph is finite. `Growth[Int]` -> `Growth[List[Int]]` -> `Growth[List[List[Int]]]` never repeats, so there is no
  // cycle to find; before the depth bound this hung the compiler outright rather than failing it.
  test("a type whose arguments grow at every step is refused instead of hanging the compiler"):
    val errors = typeCheckErrors("final case class H(g: DelGrowth[Int]) derives PrettyPrintable").map(_.message)
    assert(errors.nonEmpty, "an argument-growing recursive type must terminate the scan")
    assert(errors.exists(_.contains("cannot prove")), errors.mkString("\n"))

  test("the refusal explains that the shape is unprovable rather than annotated"):
    val errors = typeCheckErrors("final case class H(g: DelGrowth[Int]) derives PrettyPrintable").map(_.message)
    assert(errors.exists(_.contains("still growing")), errors.mkString("\n"))
    assert(errors.exists(_.contains("derives PrettyPrintable")), errors.mkString("\n"))

  test("giving the growing type an instance breaks the scan out of it"):
    given PrettyPrintable[DelGrowth[Int]] = PrettyPrintable.FromFunction((v, _) => "grown")
    final case class H(g: DelGrowth[Int]) derives PrettyPrintable
    assert(PrettyPrintable.derived[H].describe(H(DelGrowth(None, "t"))) == "H(g = grown)")

  test("an ordinary recursive type still scans clean, because it does cycle"):
    assert(show(DelHoldsRecursive(DelRecursive(Nil))) == "DelHoldsRecursive(r = DelRecursive(List()))")

  // ------------------------------- tuples are containers, not nested domain types

  // A tuple is a case class, so the delegation rule would apply to it — but nobody can write `derives PrettyPrintable` on
  // Tuple2, so delegating would strip its structure permanently AND bypass the instances of its elements. Tuples are
  // therefore expanded wherever they appear, like the collections they resemble.
  test("a tuple field is expanded rather than handed to its own toString"):
    assert(show(DelTupleHolder((1, "a"))) == """DelTupleHolder(p = Tuple2(_1 = 1, _2 = "a"))""")

  test("a tuple's elements go back through the normal resolution, so element instances still apply"):
    assert(show(DelTupleOfInstanced((1, DelInstanced(2, "v")))) ==
      """DelTupleOfInstanced(p = Tuple2(_1 = 1, _2 = DelInstanced(a = 2, s = "v")))""")

  test("an element without an instance still falls back to toString inside a tuple"):
    assert(show(DelTupleOfPlain((1, DelPlain(2, "v")))) == "DelTupleOfPlain(p = Tuple2(_1 = 1, _2 = DelPlain(2,v)))")

  test("a tuple hiding a redacting element is refused, not printed"):
    val errors = typeCheckErrors("final case class H(p: (String, DelSecret)) derives PrettyPrintable").map(_.message)
    assert(errors.nonEmpty, "a redacting element inside a tuple must not reach toString")

  test("a three element tuple expands the same way"):
    assert(show(DelTriple((1, "a", true))) == """DelTriple(t = Tuple3(_1 = 1, _2 = "a", _3 = true))""")

  // ------------------------------- collections of case classes

  test("a list of a case class with an instance renders each element structurally"):
    assert(show(DelListInstanced(List(DelInstanced(1, "v")))) ==
      """DelListInstanced(xs = [DelInstanced(a = 1, s = "v")])""")

  test("a list of a case class without an instance renders each element with toString"):
    assert(show(DelListPlain(List(DelPlain(1, "v")))) == "DelListPlain(xs = [DelPlain(1,v)])")

  test("a map value keeps the same rule"):
    assert(show(DelMapInstanced(Map("k" -> DelInstanced(1, "v")))) ==
      """DelMapInstanced(m = ["k" -> DelInstanced(a = 1, s = "v")])""")

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

// Argument-growing recursion: each step applies List to the parameter, so no two instantiations are ever equal and
// there is no cycle for the scan to detect. Bounded by MaxScanDepth rather than by cycle detection.
final case class DelGrowth[A](next: Option[DelGrowth[List[A]]], tag: String)

// Ordinary recursion for contrast: this one does cycle, so it scans clean and delegates.
final case class DelRecursive(children: List[DelRecursive])
final case class DelHoldsRecursive(r: DelRecursive) derives PrettyPrintable

final case class DelTupleHolder(p: (Int, String)) derives PrettyPrintable
final case class DelTupleOfInstanced(p: (Int, DelInstanced)) derives PrettyPrintable
final case class DelTupleOfPlain(p: (Int, DelPlain)) derives PrettyPrintable
final case class DelTriple(t: (Int, String, Boolean)) derives PrettyPrintable
final case class DelListInstanced(xs: List[DelInstanced]) derives PrettyPrintable
final case class DelListPlain(xs: List[DelPlain]) derives PrettyPrintable
final case class DelMapInstanced(m: Map[String, DelInstanced]) derives PrettyPrintable
