package com.worxbend.meeter

import com.typesafe.config.ConfigFactory
import com.worxbend.meeter.MeeterAppModule.res
import zio.config.typesafe.TypesafeConfigProvider
import zio.logging.{consoleJsonLogger, logMetrics}
import zio.metrics.jvm.DefaultJvmMetrics
import zio.{ZLayer, _}

object MeeterApp extends ZIOAppDefault:

  private val config         = ConfigFactory.load()
  private val configProvider = TypesafeConfigProvider.fromTypesafeConfig(config)

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

  override def run: ZIO[
    Any,
    Any,
    Any,
  ] =
    (for {
      _ <-
        (for {
          _ <- ZIO.log("Meow")
          r <- ZIO.serviceWithZIO[MeetLinkGeneratorService](_.generateLink()).flatMap(link => ZIO.log(link))
        } yield r).repeat(Schedule.spaced(1.seconds).forever)
    } yield ()).provide(res)
