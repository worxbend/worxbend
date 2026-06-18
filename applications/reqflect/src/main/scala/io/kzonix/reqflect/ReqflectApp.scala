package io.kzonix.reqflect

import zio.Runtime
import zio.ZIO
import zio.ZIOAppDefault
import zio.ZLayer
import zio.config.typesafe.TypesafeConfigProvider
import zio.http.Client
import zio.http.Server
import zio.logging.consoleJsonLogger
import zio.logging.logMetrics
import zio.metrics.connectors.prometheus.prometheusLayer
import zio.metrics.connectors.prometheus.publisherLayer
import zio.metrics.jvm.DefaultJvmMetrics

import io.kzonix.reqflect.AppModule.*

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
      >>> DefaultJvmMetrics.liveV2
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
      ZLayer.succeed(Server.Config.default),
      Server.live,
      Client.default,
    )

}
