package io.kzonix.nerdgalaxy

import akka.http.scaladsl.server.Route
import sttp.capabilities
import sttp.capabilities.akka.AkkaStreams
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import scala.concurrent.Future

class AkkaHttpApplicationRouter(
    endpoints: Set[ServerEndpoints],
    serverInterpreter: AkkaHttpServerInterpreter)
    extends ApplicationRouter {

  private val allEndpoints: List[ServerEndpoint[AkkaStreams with capabilities.WebSockets, Future]] =
    endpoints.flatMap(_.endpoints).toList

  val swaggerEndpoints: Seq[ServerEndpoint[Any, Future]] = SwaggerInterpreter().fromServerEndpoints[Future](
    endpoints = allEndpoints,
    "Board Game Geek API",
    "1.0",
  )

  def routes: Route = serverInterpreter.toRoute(allEndpoints ++ swaggerEndpoints)

}
