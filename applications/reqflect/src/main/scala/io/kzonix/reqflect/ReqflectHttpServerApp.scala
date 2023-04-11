package io.kzonix.reqflect

import io.kzonix.reqflect.routes.MetricsHttpMiddleware.metricsMiddleware
import io.kzonix.reqflect.routes.{MetricsRoutes, ServerInfoRoutes}
import izumi.reflect.dottyreflection.ReflectionUtil.reflectiveUncheckedNonOverloadedSelectable
import zio.*
import zio.http.*
import zio.metrics.Metric.Counter
import zio.metrics.{Metric, MetricKeyType, MetricLabel, MetricState}
import zio.metrics.connectors.prometheus.*

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
