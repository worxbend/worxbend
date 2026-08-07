package com.worxbend.describo

/** Mixin that replaces `toString` with the derived rendering.
  *
  * Both members are deliberately prefixed. They live in the same namespace as the mixing class's own fields, so a
  * one-letter name such as `p` or `c` makes any case class with a field of that name fail to compile.
  *
  * The deferred `Configuration` is resolved at the class-definition site, so a type mixing this in renders with one
  * fixed configuration. Tests that vary configuration must call the typeclass directly.
  */
trait AutoToString:

  protected given describoPrintable: Printable[this.type] = scala.compiletime.deferred

  protected given describoConfiguration: Configuration = scala.compiletime.deferred

  override def toString: String = describoPrintable.asString(this)(using describoConfiguration)
