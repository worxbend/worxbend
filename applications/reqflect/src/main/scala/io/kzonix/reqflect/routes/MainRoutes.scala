package io.kzonix.reqflect.routes

import zio.ZIO
import zio.http.*
import zio.json.*

import io.kzonix.reqflect.routes.models.NetworkInterfaceResponse
import io.kzonix.reqflect.routes.models.ServerInfoResponse
import io.kzonix.reqflect.services.ServerInfoProviderService

class MainRoutes(serverConfig: Server.Config, serverInfoProviderService: ServerInfoProviderService)
    extends AppRoutes[Client, Throwable] {

  override def routes: Routes[Client, Throwable] =
    Routes(
      Method.GET / "info" -> handler { (req: Request) =>
        getServerInfo(req).map(payload => Response.json(payload.toJson))
      }
    )

  private def getServerInfo(req: Request): ZIO[
    Any,
    Throwable,
    ServerInfoResponse,
  ] = {
    val remoteAddress                = req.remoteAddress.map(_.toString).getOrElse("unknown")
    val path                         = req.path.encode
    val host                         = req.url.host.getOrElse("unknown")
    val method                       = req.method.toString
    val headers: Map[String, String] = req.headers.map(h => (h.headerName, h.renderedValue)).toMap

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

  def make(serverConfig: Server.Config, serverInfoProviderService: ServerInfoProviderService) =
    new MainRoutes(
      serverConfig,
      serverInfoProviderService,
    )

}
