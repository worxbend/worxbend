package io.kzonix.cetus

import zio.*
import zio.http.*
import zio.metrics.Metric
import zio.metrics.Metric.Counter
import zio.metrics.MetricKeyType
import zio.metrics.MetricLabel
import zio.metrics.MetricState
import zio.metrics.connectors.prometheus.*

import io.kzonix.cetus.routes.MetricsHttpMiddleware.metricsMiddleware
import io.kzonix.cetus.routes.MetricsRoutes
import io.kzonix.cetus.routes.ServerInfoRoutes

import java.time.temporal.ChronoUnit

case class CetusHttpApp(demoRoutes: ServerInfoRoutes, metricsRoutes: MetricsRoutes) {

  private def routes: HttpApp[Client & PrometheusPublisher, Throwable] =
    (demoRoutes.routes ++ metricsRoutes.routes)
      @@ (HttpAppMiddleware.timeout(5.seconds) ++ metricsMiddleware)

  def start: URIO[Client & PrometheusPublisher & Server, Nothing] = Server.serve(routes.withDefaultErrorResponse)

}
