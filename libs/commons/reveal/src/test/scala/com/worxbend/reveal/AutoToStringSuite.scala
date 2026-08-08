package com.worxbend.reveal

import com.worxbend.reveal.annotations.Redacted

import org.scalatest.funsuite.AnyFunSuite

/** The mixin resolves its deferred `PrettyPrintable` and `Configuration` at the class definition site, so both the fixtures
  * and the `given Configuration` they see live here, inside one object. Keeping the given out of the package's top
  * level is what lets the other suites prove that implicit search for a `Configuration` can still fail.
  */
object AutoToStringFixtures:

  given configuration: Configuration = Configuration(multilineIfFieldsAreGreaterOrEqual = -1)

  final case class Mixin(p: String, c: Int) extends AutoToString

  final case class MixinSecret(@Redacted password: String, name: String) extends AutoToString

  final case class MixinHolder(inner: MixinSecret) extends AutoToString

final class AutoToStringSuite extends AnyFunSuite:

  test("the AutoToString mixin replaces toString"):
    assert(AutoToStringFixtures.Mixin("v", 3).toString == "Mixin(p = \"v\", c = 3)")

  test("the AutoToString mixin tolerates fields named p and c"):
    assert(AutoToStringFixtures.Mixin("v", 3).toString.contains("p = \"v\", c = 3"))

  test("the AutoToString mixin redacts"):
    assert(
      AutoToStringFixtures.MixinSecret("s3cret", "bob").toString ==
        "MixinSecret(password = <redacted>, name = \"bob\")"
    )

  test("the AutoToString mixin composes, so redaction survives nesting"):
    assert(
      AutoToStringFixtures.MixinHolder(AutoToStringFixtures.MixinSecret("s3cret", "bob")).toString ==
        "MixinHolder(inner = MixinSecret(password = <redacted>, name = \"bob\"))"
    )

  test("the mixin uses the Configuration in scope at the class definition site"):
    assert(!AutoToStringFixtures.Mixin("v", 3).toString.contains("\n"))
