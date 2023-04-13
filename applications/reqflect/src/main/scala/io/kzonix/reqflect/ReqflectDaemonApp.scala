package io.kzonix.reqflect

import io.kzonix.reqflect.routes.MetricsHttpMiddleware.metricsMiddleware
import io.kzonix.reqflect.routes.MetricsRoutes
import io.kzonix.reqflect.services.ServerInfoProviderService
import zio.*
import zio.http.*
import zio.http.model.Method
import zio.json.*
import zio.logging.*
import zio.metrics.*
import zio.metrics.Metric.Counter
import zio.metrics.connectors.MetricsConfig
import zio.metrics.connectors.prometheus.*
import zio.metrics.jvm.DefaultJvmMetrics

import java.time.temporal.ChronoUnit
import scala.util.Try

class ReqflectDaemonApp(serverInfoProviderService: ServerInfoProviderService) {

  def start: ZIO[Client, Throwable, Unit] =
    for {
      _ <- ZIO.logInfo("Starting scheduler")
      iteration <- Ref.make(1)
      _ <-
        (for {
          i <- iteration.get
          _ <- ZIO.logInfo(s"Iteration $i")
          _ <- iteration.set(i + 1)
          response <- makeReq()
          serverInfo <- serverInfoProviderService.getSystemInfo().either
          _ <- ZIO.logInfo(s"$response")
          _ <- ZIO.logInfo(s"$serverInfo")
        } yield ()).repeat(Schedule.spaced(5.seconds))
    } yield ()

  private def makeReq() = {
    val url = "https://distrowatch.com/news/dw.xml"

    Client
      .request(
        url,
        addZioUserAgentHeader = true,
      )
      .map(resp => resp.server.getOrElse("unknown"))
      .onError(e =>
        ZIO.logErrorCause(
          "ClientException:",
          e,
        ) &> ZIO.succeed("unknown"))
  }

}

object ReqflectDaemonApp:
  def make(serverInfoProviderService: ServerInfoProviderService) =
    new ReqflectDaemonApp(serverInfoProviderService: ServerInfoProviderService)
