package io.kzonix.cetus

import zio.ZIO
import zio.ZLayer
import zio.ZLayer.*
import zio.durationInt
import zio.http.ServerConfig
import zio.metrics.connectors.MetricsConfig

object AppModule {

  val startupVerificationLayer: ZLayer[Any, Nothing, Unit]                     = ZLayer.fromZIO(
    ZIO.log("Started effectful workflow to customize runtime configuration")
  )
  val metricsConfig: ZLayer[Any, Nothing, MetricsConfig]                       = ZLayer.fromZIO(ZIO.succeed(MetricsConfig(5.seconds)))
  val metricsRoutes: ZLayer[Any, Nothing, MetricsRoutes]                       = ZLayer.fromFunction(MetricsRoutes.apply _)
  val serverInfoRoutes: ZLayer[ServerConfig, Nothing, ServerInfoRoutes] = ZLayer.fromFunction(ServerInfoRoutes.apply _)
  val httpApp: ZLayer[MetricsRoutes & ServerInfoRoutes & ServerConfig, Nothing, CetusHttpApp] = ZLayer.fromFunction(
    CetusHttpApp.apply _)
  val daemonApp: ZLayer[Any, Nothing, CetusDaemonApp]                          = ZLayer.fromFunction(CetusDaemonApp.apply _)

}