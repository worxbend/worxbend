package io.kzonix.reqflect

import io.kzonix.reqflect.routes.MainRoutes
import io.kzonix.reqflect.routes.MetricsHttpMiddleware.metricsMiddleware
import io.kzonix.reqflect.routes.MetricsRoutes

import zio.*
import zio.http.*
import zio.metrics.Metric
import zio.metrics.Metric.Counter
import zio.metrics.MetricKeyType
import zio.metrics.MetricLabel
import zio.metrics.MetricState
import zio.metrics.connectors.prometheus.*

import java.time.temporal.ChronoUnit

class ReqflectHttpServerApp(demoRoutes: MainRoutes, metricsRoutes: MetricsRoutes):

  private type Requirements = Client & PrometheusPublisher

  def start: URIO[Client & PrometheusPublisher & Server, Nothing] = Server.serve(routes.withDefaultErrorResponse)

  private def routes: HttpApp[Requirements, Throwable] =
    (demoRoutes.routes ++ metricsRoutes.routes)
      @@ (HttpAppMiddleware.timeout(5.seconds) ++ metricsMiddleware)

object ReqflectHttpServerApp:
  def make(demoRoutes: MainRoutes, metricsRoutes: MetricsRoutes) =
    new ReqflectHttpServerApp(
      demoRoutes,
      metricsRoutes,
    )
