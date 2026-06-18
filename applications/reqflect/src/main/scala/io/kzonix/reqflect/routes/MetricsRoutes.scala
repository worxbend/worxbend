package io.kzonix.reqflect.routes

import zio.ZIO
import zio.http.*
import zio.metrics.connectors.prometheus.PrometheusPublisher

class MetricsRoutes extends AppRoutes[PrometheusPublisher, Throwable] {

  override def routes: Routes[PrometheusPublisher, Throwable] =
    Routes(
      Method.GET / "metrics" -> handler { (_: Request) =>
        for {
          publisher <- ZIO.service[PrometheusPublisher]
          metrics   <- publisher.get
        } yield Response.text(metrics)
      }
    )

}

object MetricsRoutes {
  def make() = new MetricsRoutes()
}
