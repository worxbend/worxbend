package io.kzonix.reqflect

import io.kzonix.reqflect.AppModule.*
import io.kzonix.reqflect.routes.*

import zio.*
import zio.config.typesafe.TypesafeConfigProvider
import zio.http.*
import zio.http.model.Method
import zio.json.*
import zio.logging.*
import zio.metrics.*
import zio.metrics.Metric.Counter
import zio.metrics.connectors.MetricsConfig
import zio.metrics.connectors.prometheus.PrometheusPublisher
import zio.metrics.connectors.prometheus.prometheusLayer
import zio.metrics.connectors.prometheus.publisherLayer
import zio.metrics.jvm.DefaultJvmMetrics

import scala.util.Try

import com.typesafe.config.ConfigFactory

object ReqflectApp extends ZIOAppDefault {

  override val bootstrap: ZLayer[
    Any,
    Any,
    Any,
  ] =
    Runtime.removeDefaultLoggers
      >>> Runtime.setConfigProvider(configProvider)
      >>> consoleJsonLogger()
      >>> logMetrics
      >>> DefaultJvmMetrics.live
      >>> startupVerificationLayer
  private val config         = ConfigFactory.load()
  private val configProvider = TypesafeConfigProvider.fromTypesafeConfig(config)

  override def run: ZIO[
    Any,
    Any,
    Any,
  ] =
    (for {
      httpApp   <- ZIO.service[ReqflectHttpServerApp]
      daemonApp <- ZIO.service[ReqflectDaemonApp]
      _         <- httpApp.start.zipPar(daemonApp.start)
    } yield ()).provide(
      daemonApp,
      httpApp,
      serverInfoRoutes,
      metricsRoutes,
      metricsConfig,
      prometheusLayer,
      publisherLayer,
      serverInfoCache,
      cacheAwareServerInfoProviderService,
      ServerConfig.live,
      Server.live,
      Client.default,
    )

}
