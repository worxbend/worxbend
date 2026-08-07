package com.worxbend.tck

/** What a renderer must supply to be checked against the kit.
  *
  * An adapter owns the fixture values, because each module reaches its typeclass instances differently and a fixture
  * therefore cannot be declared once in shared code. What *is* shared — and what the kit exists to pin — is the
  * expected output.
  */
trait TckAdapter:

  /** A short name for the renderer under test, used in failure messages. */
  def rendererName: String

  /** Builds the value named by `fixtureId` and renders it under `configuration`.
    *
    * Must throw [[TckAdapter.UnknownFixture]] for an id it does not implement, so that a fixture added to the kit
    * fails loudly in every adapter instead of being silently skipped.
    */
  def render(fixtureId: String, configuration: TckConfiguration): String

object TckAdapter:

  /** Thrown by an adapter asked for a fixture it does not implement. */
  final class UnknownFixture(fixtureId: String)
      extends RuntimeException(
        s"no fixture is registered for id '$fixtureId'; add it to this adapter or remove it from the TCK catalogue"
      )
