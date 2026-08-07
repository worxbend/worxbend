package com.worxbend.describo.annotations

import scala.annotation.StaticAnnotation
import scala.annotation.meta.field

/** Replaces the annotated field's value with [[replacement]] in the rendered output.
  *
  * A redacted field is never dereferenced, so a `null` redacted field prints its replacement rather than throwing, and
  * the secret never reaches the renderer. The field's declared type is still printed under `useTypeNames`, because the
  * type comes from the typeclass instance rather than from the value.
  *
  * [[Excluded]] wins over this annotation. If a field carries several `@Redacted` annotations the first one in
  * declaration order is used.
  *
  * @param replacement
  *   the literal text printed in place of the value
  */
@field
final class Redacted(val replacement: String = "<redacted>") extends StaticAnnotation
