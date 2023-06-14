package io.kzonix.cetus.routes

import io.kzonix.cetus.routes.models.ServerInfoResponse

import zio.*
import zio.ExitCode
import zio.Runtime
import zio.Scope
import zio.ZIO
import zio.ZIOAppArgs
import zio.ZLayer
import zio.http.*
import zio.http.Server.*
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

class ServerInfoRoutes(serverConfig: Server) extends AppRoutes[Client, Throwable] {

  override def routes: HttpApp[Client, Throwable] =
    Http.collectZIO {
      case req @ Method.GET -> !! / "info" =>
        for {
          response <- ZIO.succeed(Response.json(getServerInfo(req).toJson))
        } yield response
    }

  private def getServerInfo(req: Request) = {
    val ips    = req.remoteAddress.map(_.toString).getOrElse("unknown")
    val path   = req.path.encode
    val method = req.method.toString

    ServerInfoResponse(
      id = ips,
      hostname = ips,
      url = path,
      method = method,
      ip = List(ips),
      headers = Map.empty,
      host = serverConfig.port.toString,
      remoteAddr = ips,
    )

  }

}

object ServerInfoRoutes {
  def apply(serverConfig: Server) = new ServerInfoRoutes(serverConfig)
}
