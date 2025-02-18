package io.kzonix.reqflect.routes

import zio.*
import zio.http.*
import zio.http.model.Method
import zio.json.*
import zio.logging.*
import zio.metrics.connectors.prometheus.PrometheusPublisher

class MetricsRoutes extends AppRoutes[PrometheusPublisher, Throwable] {

  override def routes: HttpApp[PrometheusPublisher, Throwable] =
    Http.collectZIO {
      case _ @Method.GET -> !! / "metrics" =>
        for {
          publisher <- ZIO.service[PrometheusPublisher]
          metrics   <- publisher.get
          response  <- ZIO.succeed(Response.text(metrics))
        } yield response
    }

}

object MetricsRoutes {
  def make() = new MetricsRoutes()
}
