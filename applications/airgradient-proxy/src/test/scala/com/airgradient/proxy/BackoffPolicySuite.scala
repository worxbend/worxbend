package com.airgradient.proxy

import com.airgradient.proxy.config.PollingConfig
import com.airgradient.proxy.polling.BackoffPolicy
import munit.FunSuite

import scala.concurrent.duration.*

class BackoffPolicySuite extends FunSuite:

  private val pollingCfg = PollingConfig(
    interval     = 5.seconds,
    initialFetch = true,
    jitter       = 250.millis,
    maxBackoff   = 60.seconds,
  )

  private val backoff = new BackoffPolicy(pollingCfg)

  test("0-3 failures use normal interval") {
    assertEquals(backoff.nextDelay(0), 5.seconds)
    assertEquals(backoff.nextDelay(1), 5.seconds)
    assertEquals(backoff.nextDelay(3), 5.seconds)
  }

  test("4-10 failures use longer delay") {
    for f <- 4 to 10 do
      val d = backoff.nextDelay(f)
      assert(d.toMillis >= 10000L, s"expected >= 10s at $f failures, got $d")
      assert(d.toMillis <= 60250L, s"expected <= maxBackoff+jitter at $f failures, got $d")
  }

  test("more than 10 failures cap at maxBackoff") {
    for f <- Seq(11, 20, 100) do
      val d = backoff.nextDelay(f)
      assert(d.toMillis <= pollingCfg.maxBackoff.toMillis, s"expected <= 60s at $f failures, got $d")
  }
