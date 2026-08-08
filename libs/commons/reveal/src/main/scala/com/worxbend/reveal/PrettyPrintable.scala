package com.worxbend.reveal

/** Renders a value of `T` as a configurable, redaction-aware string.
  *
  * Instances are produced entirely at compile time by [[PrettyPrintable.derived]]: there is no reflection, no `Mirror`
  * traversal and no per-call map building, and this module has no runtime dependencies at all.
  */
trait PrettyPrintable[T]:

  /** Renders `value` under `conf`. */
  def describe(value: T)(using conf: Configuration): String

  extension (x: T)

    /** Renders `x` under the ambient configuration, or [[Configuration.default]] when none is in scope. */
    def asString(
        using conf: Configuration = Configuration.default
    ): String = describe(x)(using conf)

object PrettyPrintable:

  /** Summons the instance for `T`. */
  def apply[T](
      using describe: PrettyPrintable[T]
  ): PrettyPrintable[T] = describe

  /** Derives an instance for a case class, case object, sealed trait or enum.
    *
    * Usable as a `derives PrettyPrintable` clause. Everything the macro can decide — which fields are omitted, which are
    * redacted, how each field's declared type is spelled — is decided once, at expansion time.
    */
  inline def derived[T]: PrettyPrintable[T] = ${ PrettyPrintableMacro.deriveImpl[T] }

  /** Makes [[AutoToString]] work without any ceremony at the mixing-in class. */
  inline given autoPrettyPrintable[T <: AutoToString]: PrettyPrintable[T] = derived[T]

  /** Instance backed by a function. Public only because macro-generated code constructs it. */
  final class FromFunction[T](render: (T, Configuration) => String) extends PrettyPrintable[T]:
    override def describe(value: T)(using conf: Configuration): String = render(value, conf)

/** Mixin that replaces `toString` with the derived rendering.
  *
  * Members are prefixed rather than short so that they can never collide with a user's field names — a case class with
  * fields called `p` and `c` still compiles.
  */
trait AutoToString:

  protected given revealPrettyPrintable: PrettyPrintable[this.type] = scala.compiletime.deferred

  protected given revealConfiguration: Configuration = scala.compiletime.deferred

  override def toString: String = revealPrettyPrintable.describe(this)(using revealConfiguration)

/** Low-ceremony shim for `override def toString: String = ToString.derived(this)`.
  *
  * It renders in place, without materialising a [[PrettyPrintable]] instance, and is the original entry point of this module.
  * Prefer `derives PrettyPrintable` or [[AutoToString]] in new code: an instance composes, a rendered string does not.
  */
object ToString:

  /** Renders `value` at the call site. */
  inline def derived[T](
      inline value: T
  )(
      using inline conf: Configuration = Configuration.default
  ): String = ${ PrettyPrintableMacro.toStringImpl[T]('value, 'conf) }
