package io.kzonix.reqflect

import com.typesafe.config.ConfigFactory
import io.kzonix.reqflect.AppModule.*
import io.kzonix.reqflect.routes.*
import izumi.reflect.dottyreflection.ReflectionUtil.reflectiveUncheckedNonOverloadedSelectable
import zio.*
import zio.config.typesafe.TypesafeConfigProvider
import zio.http.*
import zio.http.model.Method
import zio.json.*
import zio.logging.*
import zio.metrics.*
import zio.metrics.Metric.Counter
import zio.metrics.connectors.MetricsConfig
import zio.metrics.connectors.prometheus.{PrometheusPublisher, prometheusLayer, publisherLayer}
import zio.metrics.jvm.DefaultJvmMetrics

import scala.util.Try

object ReqflectApp extends ZIOAppDefault {

  private val config         = ConfigFactory.load()
  private val configProvider = TypesafeConfigProvider.fromTypesafeConfig(config)

  override val bootstrap: ZLayer[Any, Any, Any] =
    Runtime.removeDefaultLoggers
      >>> Runtime.setConfigProvider(configProvider)
      >>> consoleJsonLogger()
      >>> logMetrics
      >>> DefaultJvmMetrics.live
      >>> startupVerificationLayer

  override def run: ZIO[Any, Any, Any] =
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
      serverInfoProviderService,
      ServerConfig.live,
      Server.live,
      Client.default,
    )

}
