package com.airgradient.proxy.polling

import com.airgradient.proxy.config.PollingConfig

import scala.concurrent.duration.*

final class BackoffPolicy(pollingConfig: PollingConfig):

  def nextDelay(consecutiveFailures: Int): FiniteDuration =
    if consecutiveFailures <= 3 then
      pollingConfig.interval
    else if consecutiveFailures <= 10 then
      val base    = 10.seconds
      val jitter  = (Math.random() * pollingConfig.jitter.toMillis).toLong.millis
      val backoff = base + jitter
      if backoff > pollingConfig.maxBackoff then pollingConfig.maxBackoff else backoff
    else
      val jitter  = (Math.random() * pollingConfig.jitter.toMillis).toLong.millis
      val backoff = pollingConfig.maxBackoff + jitter
      if backoff > pollingConfig.maxBackoff then pollingConfig.maxBackoff else backoff
