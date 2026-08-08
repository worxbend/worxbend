package com.worxbend.prettyprinto

/** The declared type of a value, as it should be printed.
  *
  * Carried by the typeclass instance rather than derived from a runtime class, so that rendering a field's type never
  * requires dereferencing the field. That is what makes `@Redacted` and `@Excluded` fields safe and what keeps `null`
  * fields from throwing.
  *
  * @param simpleName
  *   the dealiased, widened type name with type arguments dropped, for example `List`
  * @param qualifiedName
  *   `TypeRepr.of[X].dealias.typeSymbol.fullName` with a trailing `$` stripped, for example
  *   `scala.collection.immutable.List`
  */
final case class PrintedType(simpleName: String, qualifiedName: String):

  /** The spelling selected by `fullyQualifiedClassName` and `shortPackagePrefix`. */
  def render(using conf: Configuration): String =
    if !conf.fullyQualifiedClassName then simpleName
    else if conf.shortPackagePrefix then PrintedType.compressPackage(qualifiedName)
    else qualifiedName

object PrintedType:

  /** Compresses every leading segment whose first character is lowercase to that single character.
    *
    * `com.worxbend.prettyprinto.Fixture` becomes `c.w.d.Fixture`; a name with no package, such as `Fixture`, is returned
    * unchanged and never gains a leading dot.
    */
  def compressPackage(qualifiedName: String): String =
    val parts      = qualifiedName.split('.').toVector
    val isPackage  = (part: String) => part.headOption.exists(_.isLower)
    val compressed = parts.takeWhile(isPackage).map(_.take(1))
    val untouched  = parts.dropWhile(isPackage)
    (compressed ++ untouched).mkString(".")
