package io.kzonix.cetus

import com.typesafe.config.ConfigFactory
import io.kzonix.cetus.AppModule.*
import izumi.distage.model.definition.ModuleDef
import zio.*
import zio.config.typesafe.TypesafeConfigProvider
import zio.http.*
import zio.http.Server.Config
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

object CetusApp extends ZIOAppDefault {
  private val config = ConfigFactory.load()
  private val configProvider = TypesafeConfigProvider.fromTypesafeConfig(config)


  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers
      >>> Runtime.setConfigProvider(configProvider)
      >>> consoleJsonLogger()
      >>> DefaultJvmMetrics.live
      >>> startupVerificationLayer

  
  override def run: ZIO[Any, Any, Any] =
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
