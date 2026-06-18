package io.kzonix.reqflect

import zio.*
import zio.http.*
import zio.metrics.connectors.prometheus.PrometheusPublisher

import io.kzonix.reqflect.routes.{ MainRoutes, MetricsRoutes }
import io.kzonix.reqflect.routes.MetricsHttpMiddleware.metricsMiddleware

class ReqflectHttpServerApp(demoRoutes: MainRoutes, metricsRoutes: MetricsRoutes):

  private type Requirements = Client & PrometheusPublisher

  def start: URIO[Client & PrometheusPublisher & Server, Nothing] = Server.serve(routes.sandbox)

  private def routes: Routes[Requirements, Throwable] = {
    val combined: Routes[Requirements, Throwable] =
      demoRoutes.routes ++ metricsRoutes.routes
    combined @@ Middleware.timeout(5.seconds) @@ metricsMiddleware
  }

object ReqflectHttpServerApp:

  def make(demoRoutes: MainRoutes, metricsRoutes: MetricsRoutes) =
    new ReqflectHttpServerApp(
      demoRoutes,
      metricsRoutes,
    )
