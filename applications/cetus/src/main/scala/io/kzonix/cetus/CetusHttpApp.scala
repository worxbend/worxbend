package io.kzonix.cetus

import zio.*
import zio.http.*
import zio.metrics.connectors.prometheus.PrometheusPublisher

import io.kzonix.cetus.routes.MetricsHttpMiddleware.metricsMiddleware
import io.kzonix.cetus.routes.MetricsRoutes
import io.kzonix.cetus.routes.ServerInfoRoutes

case class CetusHttpApp(demoRoutes: ServerInfoRoutes, metricsRoutes: MetricsRoutes) {

  private def routes: Routes[Client & PrometheusPublisher, Throwable] = {
    val combined: Routes[Client & PrometheusPublisher, Throwable] =
      demoRoutes.routes ++ metricsRoutes.routes
    combined @@ Middleware.timeout(5.seconds) @@ metricsMiddleware
  }

  def start: URIO[Client & PrometheusPublisher & Server, Nothing] = Server.serve(routes.sandbox)

}
