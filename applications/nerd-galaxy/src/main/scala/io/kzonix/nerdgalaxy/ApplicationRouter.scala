package io.kzonix.nerdgalaxy

trait ApplicationRouter {

  import akka.http.scaladsl.server.Route

  def routes: Route

}
