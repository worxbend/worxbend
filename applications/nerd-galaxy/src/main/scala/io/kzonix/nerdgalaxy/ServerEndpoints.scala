package io.kzonix.nerdgalaxy

trait ServerEndpoints {

  import sttp.capabilities.akka.AkkaStreams
  import sttp.capabilities.WebSockets
  import sttp.tapir.server.ServerEndpoint

  import scala.concurrent.Future

  def endpoints: List[ServerEndpoint[AkkaStreams with WebSockets, Future]]

}
