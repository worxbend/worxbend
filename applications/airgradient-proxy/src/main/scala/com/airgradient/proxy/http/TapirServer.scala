package com.airgradient.proxy.http

import com.airgradient.proxy.cache.FreshnessClassifier
import com.airgradient.proxy.cache.SnapshotStore
import com.airgradient.proxy.config.AppConfig
import com.airgradient.proxy.domain.CacheStatus
import com.airgradient.proxy.domain.ProxyStatus
import com.airgradient.proxy.metrics.ProxyMetrics
import io.opentelemetry.api.OpenTelemetry
import izumi.logstage.api.IzLogger
import sttp.model.Header
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.netty.sync.NettySyncServer
import sttp.tapir.server.netty.sync.NettySyncServerOptions
import sttp.tapir.server.tracing.opentelemetry.OpenTelemetryTracing

import java.time.Instant

final class TapirServer(
  config:     AppConfig,
  store:      SnapshotStore,
  classifier: FreshnessClassifier,
  metrics:    ProxyMetrics,
  otel:       OpenTelemetry,
  logger:     IzLogger,
):

  // Identity effect — no monad wrapping in direct-style Tapir Netty sync.
  private type Id[A] = A

  // Blocks until interrupted — call from a dedicated user fork.
  def startAndWait(): Unit =
    logger.info(s"Starting HTTP server on ${config.http.host}:${config.http.port}")
    val serverOptions = NettySyncServerOptions.customiseInterceptors
      .metricsInterceptor(metrics.tapirMetrics.metricsInterceptor())
      .prependInterceptor(OpenTelemetryTracing(otel))
      .options
    NettySyncServer()
      .host(config.http.host)
      .port(config.http.port)
      .options(serverOptions)
      .addEndpoints(allRoutes)
      .startAndWait()

  private def allRoutes: List[ServerEndpoint[Any, Id]] = List(
    AirGradientEndpoints.measuresCurrentEndpoint.serverLogic[Id] { _ => handleMeasuresCurrent() },
    ProxyEndpoints.healthEndpoint.serverLogicSuccess[Id] { _ => () },
    ProxyEndpoints.readyEndpoint.serverLogic[Id] { _ => handleReady() },
    ProxyEndpoints.statusEndpoint.serverLogicSuccess[Id] { _ => handleStatus() },
    ProxyEndpoints.metricsEndpoint.serverLogicSuccess[Id] { _ => metrics.scrapeText() },
    ProxyEndpoints.schemaListEndpoint.serverLogicSuccess[Id] { _ => SchemaRegistry.names },
    ProxyEndpoints.schemaByNameEndpoint.serverLogic[Id] { name =>
      SchemaRegistry.byName.get(name).toRight(sttp.model.StatusCode.NotFound)
    },
  )

  private def handleMeasuresCurrent(): Either[StatusCode, (Array[Byte], List[Header])] =
    val state = store.get()
    val now   = Instant.now()

    state.snapshot match
      case None => Left(StatusCode.ServiceUnavailable)
      case Some(snapshot) =>
        val status    = classifier.classify(snapshot, now)
        val ageMillis = classifier.cacheAgeMillis(snapshot, now)
        if status == CacheStatus.Expired && !config.cache.serveExpired then
          Left(StatusCode.ServiceUnavailable)
        else
          val proxyHeaders = List(
            Header("Content-Type", snapshot.contentType),
            Header("X-AirGradient-Proxy", "true"),
            Header("X-AirGradient-Proxy-Cache-Status", status.toString.toLowerCase),
            Header("X-AirGradient-Proxy-Cache-Age-Millis", ageMillis.toString),
          )
          Right((snapshot.payload, proxyHeaders))

  private def handleReady(): Either[StatusCode, Unit] =
    if store.get().snapshot.isDefined then Right(())
    else Left(StatusCode.ServiceUnavailable)

  private def handleStatus(): ProxyStatus =
    val state  = store.get()
    val now    = Instant.now()
    val (cacheAgeMs, cacheStatusStr) = state.snapshot match
      case None           => (None, "empty")
      case Some(snapshot) =>
        val status    = classifier.classify(snapshot, now)
        val ageMillis = classifier.cacheAgeMillis(snapshot, now)
        (Some(ageMillis), status.toString.toLowerCase)

    ProxyStatus(
      upstreamUrl            = config.upstream.baseUrl,
      lastAttemptAt          = state.lastAttemptAt.map(_.toString),
      lastSuccessAt          = state.lastSuccessAt.map(_.toString),
      lastFailureAt          = state.lastFailureAt.map(_.toString),
      consecutiveFailures    = state.consecutiveFailures,
      lastError              = state.lastError,
      cacheAgeMillis         = cacheAgeMs,
      cacheStatus            = cacheStatusStr,
      pollingIntervalMillis  = config.polling.interval.toMillis,
      requestTimeoutMillis   = config.upstream.requestTimeout.toMillis,
    )
