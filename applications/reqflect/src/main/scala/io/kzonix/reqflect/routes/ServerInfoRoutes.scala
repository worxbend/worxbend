package io.kzonix.reqflect.routes

import io.kzonix.reqflect.routes.models.ServerInfoResponse

import zio.*
import zio.http.*
import zio.http.model.Method
import zio.json.*
import zio.logging.*
import zio.metrics.*
import zio.metrics.Metric.Counter
import zio.metrics.connectors.MetricsConfig
import zio.metrics.connectors.prometheus.PrometheusPublisher
import zio.metrics.connectors.prometheus.prometheusLayer
import zio.metrics.connectors.prometheus.publisherLayer
import zio.metrics.jvm.DefaultJvmMetrics

import izumi.reflect.dottyreflection.ReflectionUtil.reflectiveUncheckedNonOverloadedSelectable

import scala.util.Try

class ServerInfoRoutes(serverConfig: ServerConfig) extends AppRoutes[Client, Throwable] {

  override def routes: HttpApp[Client, Throwable] =
    Http.collectZIO {
      case req @ Method.GET -> !! / "info" =>
        for {
          response <- ZIO.succeed(Response.json(getServerInfo(req).toJson))
        } yield response
    }

  private def getServerInfo(req: Request) = {
    val ips     = req.remoteAddress.map(_.toString).getOrElse("unknown")
    val path    = req.path.encode
    val host    = req.host.map(_.toString).getOrElse("unknown")
    val method  = req.method.toString
    val headers = req.headers.map(h => (h.key.toString, h.value.toString)).toMap

    ServerInfoResponse(
      id = host,
      hostname = host,
      url = path,
      method = method,
      ip = List(ips),
      headers = headers,
      host = serverConfig.address.getHostString,
      remoteAddr = ips,
    )

  }

}

object ServerInfoRoutes {
  def make(serverConfig: ServerConfig) = new ServerInfoRoutes(serverConfig)
}
