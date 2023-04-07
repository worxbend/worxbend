package io.kzonix.cetus

import zio.*
import zio.http.*
import zio.http.model.Method
import zio.json.*
import zio.logging.*
import zio.logging.backend.SLF4J
import zio.metrics.*
import zio.metrics.Metric.Counter
import zio.metrics.connectors.MetricsConfig
import zio.metrics.connectors.prometheus.PrometheusPublisher
import zio.metrics.connectors.prometheus.prometheusLayer
import zio.metrics.connectors.prometheus.publisherLayer
import zio.metrics.jvm.DefaultJvmMetrics

import io.kzonix.cetus.AppModule.*
import io.kzonix.cetus.routes.*

import izumi.reflect.dottyreflection.ReflectionUtil.reflectiveUncheckedNonOverloadedSelectable

import scala.util.Try

import org.slf4j.LoggerFactory

object CetusApp extends ZIOAppDefault {

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers
      >>> SLF4J.slf4j
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
      ServerConfig.live,
      Server.live,
      Client.default,
      //ZLayer.Debug.tree,
    )

}
