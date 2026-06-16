package com.airgradient.proxy.app

import com.airgradient.proxy.cache.AtomicSnapshotStore
import com.airgradient.proxy.cache.FreshnessClassifier
import com.airgradient.proxy.config.ConfigLoader
import com.airgradient.proxy.http.TapirServer
import com.airgradient.proxy.metrics.ProxyMetrics
import com.airgradient.proxy.polling.BackoffPolicy
import com.airgradient.proxy.polling.PollingLoop
import com.airgradient.proxy.upstream.SttpAirGradientClient
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk
import izumi.logstage.api.IzLogger
import izumi.logstage.api.Log
import izumi.logstage.api.rendering.RenderingOptions
import izumi.logstage.api.rendering.StringRenderingPolicy
import izumi.logstage.sink.slf4j.LogSinkLegacySlf4jImpl

import scala.util.control.NonFatal

object App:

  def run(): Unit =
    val logger = makeLogger()

    ConfigLoader.load() match
      case Left(err) =>
        logger.crit(s"Failed to load configuration: $err")
        sys.exit(1)
      case Right(cfg) =>
        logger.info(
          s"Starting airgradient-proxy: upstream=${cfg.upstream.baseUrl} port=${cfg.http.port} poll_interval=${cfg.polling.interval}"
        )
        val proxyMetrics = new ProxyMetrics()
        val otel         = initOpenTelemetry(logger)
        val store        = new AtomicSnapshotStore()
        val classifier   = new FreshnessClassifier(cfg.cache)
        val client       = new SttpAirGradientClient(cfg.upstream)
        val backoff      = new BackoffPolicy(cfg.polling)
        val pollingLoop  = new PollingLoop(client, store, cfg.polling, backoff, proxyMetrics, logger)
        val server       = new TapirServer(cfg, store, classifier, proxyMetrics, otel, logger)
        val lifecycle    = new AppLifecycle(server, pollingLoop)
        lifecycle.run()

  private def initOpenTelemetry(logger: IzLogger): OpenTelemetry =
    if sys.env.contains("OTEL_SERVICE_NAME") then
      try
        val sdk = AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk
        logger.info("OpenTelemetry SDK initialised via autoconfigure")
        sdk
      catch
        case NonFatal(e) =>
          logger.warn(s"OpenTelemetry autoconfigure failed, using noop: ${e.getMessage}")
          OpenTelemetry.noop()
    else
      logger.debug("OTEL_SERVICE_NAME not set; OpenTelemetry is noop")
      OpenTelemetry.noop()

  private def makeLogger(): IzLogger =
    val policy = new StringRenderingPolicy(RenderingOptions.default, None)
    val sink   = new LogSinkLegacySlf4jImpl(policy)
    IzLogger(Log.Level.Info, sink)
