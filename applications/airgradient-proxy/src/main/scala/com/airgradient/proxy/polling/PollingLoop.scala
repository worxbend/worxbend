package com.airgradient.proxy.polling

import com.airgradient.proxy.cache.SnapshotStore
import com.airgradient.proxy.config.PollingConfig
import com.airgradient.proxy.metrics.ProxyMetrics
import com.airgradient.proxy.upstream.AirGradientClient
import com.airgradient.proxy.upstream.UpstreamError
import izumi.logstage.api.IzLogger

import scala.concurrent.duration.*

final class PollingLoop(
  client:        AirGradientClient,
  store:         SnapshotStore,
  pollingConfig: PollingConfig,
  backoff:       BackoffPolicy,
  metrics:       ProxyMetrics,
  logger:        IzLogger,
):

  def run(): Unit =
    if pollingConfig.initialFetch then poll()
    loop()

  private def loop(): Unit =
    while !Thread.currentThread().isInterrupted do
      val state = store.get()
      val delay = backoff.nextDelay(state.consecutiveFailures)
      sleepInterruptibly(delay)
      if !Thread.currentThread().isInterrupted then poll()

  private def poll(): Unit =
    client.fetchCurrentMeasures() match
      case Right(snapshot) =>
        val wasEmpty = store.get().snapshot.isEmpty
        store.updateSuccess(snapshot)
        metrics.recordPollSuccess()
        metrics.updateConsecutiveFailures(0)
        if wasEmpty then
          logger.info(
            s"First successful upstream fetch: upstream=${snapshot.upstreamUrl} duration_ms=${snapshot.upstreamDurationMillis}"
          )
      case Left(err) =>
        store.updateFailure(err.message)
        metrics.recordPollFailure()
        metrics.updateConsecutiveFailures(store.get().consecutiveFailures)
        logger.warn(
          s"Upstream fetch failed: error_type=${err.errorType} error_message=${err.message} failure_count=${store.get().consecutiveFailures}"
        )

  private def sleepInterruptibly(duration: FiniteDuration): Unit =
    try Thread.sleep(duration.toMillis)
    catch case _: InterruptedException => Thread.currentThread().interrupt()
