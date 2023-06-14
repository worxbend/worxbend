package io.kzonix.reqflect.routes

import io.kzonix.reqflect.routes.models.NetworkInterfaceResponse
import io.kzonix.reqflect.routes.models.ServerInfoResponse
import io.kzonix.reqflect.services.ServerInfoProviderService
import io.kzonix.reqflect.services.exceptions.ReqflectServiceException
import io.kzonix.reqflect.services.exceptions.ReqflectServiceException.GeneralException

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

import scala.util.Try

class MainRoutes(serverConfig: ServerConfig, serverInfoProviderService: ServerInfoProviderService)
    extends AppRoutes[Client, Throwable] {

  override def routes: HttpApp[Client, Throwable] =
    Http.collectZIO {
      case req @ Method.GET -> !! / "info" =>
        for {
          payload  <- getServerInfo(req)
          response <- ZIO.succeed(Response.json(payload.toJson))
        } yield response
    }

  private def getServerInfo(req: Request): ZIO[
    Any,
    Throwable,
    ServerInfoResponse,
  ] = {
    val remoteAddress                = req.remoteAddress.map(_.toString).getOrElse("unknown")
    val path                         = req.path.encode
    val host                         = req.host.map(_.toString).getOrElse("unknown")
    val method                       = req.method.toString
    val headers: Map[String, String] = req.headers.map(h => (h.key.toString, h.value.toString)).toMap

    import io.github.arainko.ducktape.*

    (for {
      systemInfo         <- serverInfoProviderService.getSystemInfo()
      serverInfoResponse <- ZIO.attempt {
                              systemInfo
                                .into[ServerInfoResponse]
                                .transform(
                                  Field.computed(
                                    _.networkInterfaces,
                                    p => p.networkInterfaces.map(_.to[NetworkInterfaceResponse]),
                                  ),
                                  Field.const(
                                    _.hostname,
                                    serverConfig.address.toString,
                                  ),
                                  Field.const(
                                    _.url,
                                    path,
                                  ),
                                  Field.const(
                                    _.headers,
                                    headers,
                                  ),
                                  Field.const(
                                    _.method,
                                    method,
                                  ),
                                  Field.const(
                                    _.host,
                                    host,
                                  ),
                                  Field.const(
                                    _.remoteAddress,
                                    remoteAddress,
                                  ),
                                )
                            }
    } yield serverInfoResponse).orElseFail(RuntimeException("Opps"))
  }

}

object MainRoutes {
  def make(serverConfig: ServerConfig, serverInfoProviderService: ServerInfoProviderService) =
    new MainRoutes(
      serverConfig,
      serverInfoProviderService,
    )
}
