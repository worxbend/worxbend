package io.kzonix.reqflect

import io.kzonix.reqflect.routes.{MetricsRoutes, ServerInfoRoutes}
import io.kzonix.reqflect.services.{DefaultServerInfoProviderService, ServerInfoProviderService}
import zio.ZLayer.*
import zio.{ZIO, ZLayer, durationInt}
import zio.http.ServerConfig
import zio.metrics.connectors.MetricsConfig

object AppModule {

  private type App = MetricsRoutes & ServerInfoRoutes & ServerConfig

  val startupVerificationLayer: ZLayer[Any, Nothing, Unit] = ZLayer.fromZIO(
    ZIO.log("Started effectful workflow to customize runtime configuration")
  )
  val metricsConfig: ZLayer[Any, Nothing, MetricsConfig] = ZLayer.fromZIO(ZIO.succeed(MetricsConfig(5.seconds)))
  val metricsRoutes: ZLayer[Any, Nothing, MetricsRoutes] = ZLayer.fromFunction(MetricsRoutes.make _)
  val serverInfoRoutes: ZLayer[ServerConfig, Nothing, ServerInfoRoutes] = ZLayer.fromFunction(ServerInfoRoutes.make _)
  val httpApp: ZLayer[App, Nothing, ReqflectHttpServerApp] = ZLayer.fromFunction(ReqflectHttpServerApp.make _)

  val daemonApp: ZLayer[ServerInfoProviderService, Nothing, ReqflectDaemonApp] = ZLayer
    .fromFunction(ReqflectDaemonApp.make _)
  val serverInfoProviderService: ZLayer[Any, Nothing, ServerInfoProviderService] = ZLayer
    .fromFunction(DefaultServerInfoProviderService.make _)

}
