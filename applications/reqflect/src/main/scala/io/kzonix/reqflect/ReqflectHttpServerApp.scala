package io.kzonix.reqflect

import io.kzonix.reqflect.routes.MetricsHttpMiddleware.metricsMiddleware
import io.kzonix.reqflect.routes.MetricsRoutes
import io.kzonix.reqflect.routes.ServerInfoRoutes

import zio.*
import zio.http.*
import zio.metrics.Metric
import zio.metrics.Metric.Counter
import zio.metrics.MetricKeyType
import zio.metrics.MetricLabel
import zio.metrics.MetricState
import zio.metrics.connectors.prometheus.*

import izumi.reflect.dottyreflection.ReflectionUtil.reflectiveUncheckedNonOverloadedSelectable

import java.time.temporal.ChronoUnit

class ReqflectHttpServerApp(demoRoutes: ServerInfoRoutes, metricsRoutes: MetricsRoutes):

  private def routes: HttpApp[Client & PrometheusPublisher, Throwable] =
    (demoRoutes.routes ++ metricsRoutes.routes)
      @@ (HttpAppMiddleware.timeout(5.seconds) ++ metricsMiddleware)

  def start: URIO[Client & PrometheusPublisher & Server, Nothing] = Server.serve(routes.withDefaultErrorResponse)

object ReqflectHttpServerApp:
  def make(demoRoutes: ServerInfoRoutes, metricsRoutes: MetricsRoutes) =
    new ReqflectHttpServerApp(
      demoRoutes,
      metricsRoutes,
    )
