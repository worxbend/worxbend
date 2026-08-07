package com.worxbend.tck

/** One conformance obligation: rendering `fixtureId` under `configuration` must produce exactly `expected`.
  *
  * @param fixtureId
  *   names a value that every adapter must know how to build and render. Adapters resolve it through
  *   [[TckAdapter.render]]; the kit deliberately does not describe the value's *type*, because the two renderers reach
  *   their instances by different mechanisms (`derives Printable` against magnolia, `derives Describe` against a
  *   macro).
  * @param configuration
  *   the options the value must be rendered under.
  * @param expected
  *   the exact string. Not a prefix, not a regex — the whole point of the kit is that the two engines agree byte for
  *   byte.
  * @param note
  *   why this case exists, shown in the failure message so a break explains itself.
  */
final case class TckCase(
    fixtureId:     String,
    configuration: TckConfiguration,
    expected:      String,
    note:          String,
)
