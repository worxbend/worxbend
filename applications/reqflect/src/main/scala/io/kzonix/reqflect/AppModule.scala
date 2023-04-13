package io.kzonix.reqflect

import io.kzonix.reqflect.routes.MetricsRoutes
import io.kzonix.reqflect.routes.ServerInfoRoutes
import io.kzonix.reqflect.services.CacheAwareServerInfoProviderService
import io.kzonix.reqflect.services.DefaultServerInfoProviderService
import io.kzonix.reqflect.services.ServerInfoProviderService
import io.kzonix.reqflect.services.exceptions.ReqflectServiceException
import io.kzonix.reqflect.services.models.SystemInfo

import zio.Duration
import zio.UIO
import zio.URIO
import zio.ZIO
import zio.ZLayer
import zio.ZLayer.*
import zio.cache.Cache
import zio.cache.Lookup
import zio.durationInt
import zio.http.ServerConfig
import zio.metrics.connectors.MetricsConfig

import izumi.reflect.dottyreflection.ReflectionUtil.reflectiveUncheckedNonOverloadedSelectable

object AppModule {

  private type App             = MetricsRoutes & ServerInfoRoutes & ServerConfig
  private type ServerInfoCache = Cache[String, ReqflectServiceException, SystemInfo]

  val startupVerificationLayer: ZLayer[Any, Nothing, Unit]              = ZLayer.fromZIO(
    ZIO.log("Started effectful workflow to customize runtime configuration")
  )
  val metricsConfig: ZLayer[Any, Nothing, MetricsConfig]                = ZLayer.fromZIO(ZIO.succeed(MetricsConfig(5.seconds)))
  val metricsRoutes: ZLayer[Any, Nothing, MetricsRoutes]                = ZLayer.fromFunction(MetricsRoutes.make _)
  val serverInfoRoutes: ZLayer[ServerConfig, Nothing, ServerInfoRoutes] = ZLayer.fromFunction(ServerInfoRoutes.make _)
  val httpApp: ZLayer[App, Nothing, ReqflectHttpServerApp]              = ZLayer.fromFunction(ReqflectHttpServerApp.make _)

  val daemonApp: ZLayer[ServerInfoProviderService, Nothing, ReqflectDaemonApp] = ZLayer
    .fromFunction(ReqflectDaemonApp.make _)

  val defaultServerInfoProviderService: ZLayer[Any, Nothing, ServerInfoProviderService] = ZLayer
    .fromFunction(DefaultServerInfoProviderService.make _)

  val serverInfoCache: ZLayer[Any, Nothing, Cache[String, ReqflectServiceException, SystemInfo]] =
    defaultServerInfoProviderService >>> ZLayer.fromZIO(CacheAwareServerInfoProviderService.makeCache())

  val cacheAwareServerInfoProviderService: ZLayer[ServerInfoCache, Nothing, ServerInfoProviderService] = ZLayer
    .fromFunction(CacheAwareServerInfoProviderService.make _)

}
