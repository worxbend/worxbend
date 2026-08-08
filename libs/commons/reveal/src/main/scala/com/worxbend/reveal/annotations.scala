package com.worxbend.reveal

import scala.annotation.StaticAnnotation
import scala.annotation.meta.field

/** Field annotations understood by [[PrettyPrintable]].
  *
  * Precedence is resolved once per field, first match wins:
  *   1. `@Excluded` or `@transient` -> the field is omitted entirely;
  *   1. `@Redacted` -> the field renders its replacement and its value is never read;
  *   1. otherwise -> the field renders normally.
  *
  * Exclusion therefore beats redaction: `@Redacted @Excluded both: String` is omitted.
  */
object annotations:

  /** Omit the annotated field from the rendered output. Its value is never read. */
  @field
  final class Excluded extends StaticAnnotation

  /** Render `replacement` in place of the annotated field's value. The value is never read.
    *
    * `replacement` must be a string literal so that the macro can resolve it at compile time. When a field carries more
    * than one `@Redacted`, the first one in declaration order wins.
    */
  @field
  final class Redacted(val replacement: String = "<redacted>") extends StaticAnnotation
