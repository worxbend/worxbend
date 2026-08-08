package com.worxbend.describo

import com.worxbend.describo.annotations.Excluded
import com.worxbend.describo.annotations.Redacted

/** What the renderer does with a single field. Resolved once per field, per typeclass instance. */
private enum FieldRule:

  case Omit
  case Redact(replacement: String)
  case Render

private object FieldRule:

  /** Resolves the rule for one field. First match wins:
    *
    *   1. `@Excluded` or `@transient` -> [[FieldRule.Omit]]
    *   1. `@Redacted` -> [[FieldRule.Redact]] with that annotation's replacement
    *   1. otherwise -> [[FieldRule.Render]]
    *
    * Exclusion beats redaction, so `@Redacted @Excluded both: String` is omitted. The source order of the two
    * annotations is irrelevant: precedence is by rule, not by position.
    *
    * `scala.transient` is a serialization marker; it is honoured here only for backwards compatibility and is mapped to
    * exactly the same rule as `@Excluded`, which is the intended spelling. Mapping both in this one place is what keeps
    * the alias from drifting.
    *
    * Several `@Redacted` annotations on one field are not an error: the one written FIRST in source order wins, which
    * is the rule reveal's macro implements too. Magnolia surfaces `param.annotations` in reverse source order, so the
    * array is reversed before the search — without that, the two modules disagree on the same input.
    */
  def of(annotations: IArray[Any]): FieldRule =
    val omitted = annotations.exists:
      case _: Excluded  => true
      case _: transient => true
      case _            => false
    if omitted then FieldRule.Omit
    else
      annotations
        .reverse
        .collectFirst { case redacted: Redacted => FieldRule.Redact(redacted.replacement) }
        .getOrElse(FieldRule.Render)
