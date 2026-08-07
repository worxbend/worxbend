package com.worxbend.describo.annotations

import scala.annotation.StaticAnnotation
import scala.annotation.meta.field

/** Omits the annotated field from the rendered output entirely.
  *
  * An excluded field is never dereferenced: neither its value nor its type is read. Exclusion wins over
  * [[Redacted]] — a field carrying both annotations disappears rather than printing a replacement.
  *
  * `@scala.transient` is an exact alias of this annotation. `transient` is a serialization marker and is honoured only
  * for backwards compatibility; `@Excluded` is the intended spelling.
  */
@field
final class Excluded extends StaticAnnotation
