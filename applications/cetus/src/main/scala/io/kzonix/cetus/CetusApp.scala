package io.kzonix.cetus

import zio.*
import zio.config.typesafe.TypesafeConfigProvider
import zio.http.Client
import zio.http.DnsResolver
import zio.http.Server
import zio.logging.*
import zio.metrics.connectors.prometheus.prometheusLayer
import zio.metrics.connectors.prometheus.publisherLayer
import zio.metrics.jvm.DefaultJvmMetrics

import io.kzonix.cetus.AppModule.*

import com.typesafe.config.ConfigFactory

object CetusApp extends ZIOAppDefault {

  private val config         = ConfigFactory.load()
  private val configProvider = TypesafeConfigProvider.fromTypesafeConfig(config)

  override val bootstrap: ZLayer[
    ZIOAppArgs,
    Any,
    Any,
  ] =
    Runtime.removeDefaultLoggers
      >>> Runtime.setConfigProvider(configProvider)
      >>> consoleJsonLogger()
      >>> DefaultJvmMetrics.liveV2
      >>> startupVerificationLayer

  override def run: ZIO[
    Any,
    Any,
    Any,
  ] =
    (for {
      httpApp   <- ZIO.service[CetusHttpApp]
      daemonApp <- ZIO.service[CetusDaemonApp]
      _         <- httpApp.start.zipPar(daemonApp.start)
    } yield ()).provide(
      daemonApp,
      httpApp,
      serverInfoRoutes,
      metricsRoutes,
      metricsConfig,
      prometheusLayer,
      publisherLayer,
      DnsResolver.default,
      Client.configured(),
      Server.configured(),
      // ZLayer.Debug.tree,
    )

}
