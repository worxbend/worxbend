package io.kzonix.cetus.routes

import zio.ZIO
import zio.http.model.Method
import zio.http.Http
import zio.http.HttpApp
import zio.http.Response
import zio.metrics.connectors.prometheus.PrometheusPublisher
import org.slf4j.LoggerFactory
import zio.*
import zio.http.*
import zio.logging.*
import zio.http.model.Method
import zio.ExitCode
import zio.Runtime
import zio.Scope
import zio.ZIO
import zio.ZIOAppArgs
import zio.ZIOAppDefault
import zio.ZLayer
import zio.logging.backend.SLF4J
import zio.json.*

class MetricsRoutes extends AppRoutes[PrometheusPublisher, Throwable] {
  override def routes: HttpApp[PrometheusPublisher, Throwable] =
    Http.collectZIO {
      case _ @ Method.GET -> !! / "metrics" =>
        for {
          publisher <- ZIO.service[PrometheusPublisher]
          metrics   <- publisher.get
          response  <- ZIO.succeed(Response.text(metrics))
        } yield response
    }
}

object MetricsRoutes {
  def apply() = new MetricsRoutes()
}
