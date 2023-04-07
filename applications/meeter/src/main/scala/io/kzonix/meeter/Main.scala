package io.kzonix.meeter

import zio.*
import zio.http.*
import zio.logging.*
import zio.logging.backend.SLF4J
import zio.metrics.jvm.DefaultJvmMetrics

object MeeterApp extends ZIOAppDefault {

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers
      >>> SLF4J.slf4j
      >>> DefaultJvmMetrics.live

  override def run: ZIO[Any, Any, Any] =
    (for {
      httpApp <- ZIO.logInfo("Hello there CLI application based on ZIO")
    } yield ()).provide(
      ServerConfig.live,
      Server.live,
      Client.default,
      // ZLayer.Debug.tree,
    )

}
