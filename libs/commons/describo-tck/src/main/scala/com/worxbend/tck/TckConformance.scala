package com.worxbend.tck

import org.scalatest.funsuite.AnyFunSuite

/** Runs the whole [[Tck]] catalogue against one adapter, one scalatest case per obligation.
  *
  * A module gets its conformance suite by extending this and supplying an adapter; there is nothing else to write, and
  * nothing a module can quietly opt out of. Extending the catalogue therefore breaks every renderer that has not
  * implemented the new fixture, which is the point.
  */
abstract class TckConformance(adapter: TckAdapter) extends AnyFunSuite:

  Tck.cases.foreach: obligation =>
    test(s"[${adapter.rendererName}] ${obligation.fixtureId}: ${obligation.note}"):
      val expected = obligation.expected.replace("{pkg}", adapter.fixturePackage)
      val actual   = adapter.render(obligation.fixtureId, obligation.configuration)
      assert(
        actual == expected,
        s"""|
            |renderer  : ${adapter.rendererName}
            |fixture   : ${obligation.fixtureId}
            |obligation: ${obligation.note}
            |expected  : ${TckConformance.visible(expected)}
            |actual    : ${TckConformance.visible(actual)}
            |""".stripMargin,
      )

  test(s"[${adapter.rendererName}] the adapter implements every fixture in the catalogue"):
    val missing = Tck.fixtureIds.filter: id =>
      try
        adapter.render(id, TckConfiguration.flat)
        false
      catch case _: TckAdapter.UnknownFixture => true
    assert(missing.isEmpty, s"${adapter.rendererName} has no fixture for: ${missing.mkString(", ")}")

object TckConformance:

  /** Makes whitespace differences legible in a failure message; a mismatch that is only a newline is otherwise
    * invisible in the diff.
    */
  private def visible(s: String): String =
    if s == null then "null"
    else s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r")
