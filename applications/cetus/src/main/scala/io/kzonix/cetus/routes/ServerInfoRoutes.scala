package io.kzonix.cetus.routes

import zio.http.*
import zio.json.*

import io.kzonix.cetus.routes.models.ServerInfoResponse

class ServerInfoRoutes(serverConfig: Server) extends AppRoutes[Client, Throwable] {

  override def routes: Routes[Client, Throwable] =
    Routes(
      Method.GET / "info" -> handler { (req: Request) =>
        serverConfig.port.map(port => Response.json(getServerInfo(req, port).toJson))
      }
    )

  private def getServerInfo(req: Request, port: Int) = {
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
      host = port.toString,
      remoteAddr = ips,
    )

  }

}

object ServerInfoRoutes {
  def apply(serverConfig: Server) = new ServerInfoRoutes(serverConfig)
}
