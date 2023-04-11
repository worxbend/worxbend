package io.kzonix.reqflect

import zio.*
import zio.http.*
import zio.http.model.Method
import zio.json.*
import zio.logging.*
import zio.metrics.*
import zio.metrics.Metric.Counter
import zio.metrics.connectors.MetricsConfig
import zio.metrics.connectors.prometheus.*
import zio.metrics.connectors.prometheus.PrometheusPublisher
import zio.metrics.connectors.prometheus.prometheusLayer
import zio.metrics.connectors.prometheus.publisherLayer
import zio.metrics.jvm.DefaultJvmMetrics

import io.kzonix.reqflect.routes.MetricsHttpMiddleware.metricsMiddleware
import io.kzonix.reqflect.routes.MetricsRoutes

import izumi.reflect.dottyreflection.ReflectionUtil.reflectiveUncheckedNonOverloadedSelectable

import scala.util.Try

import java.time.temporal.ChronoUnit
case class ReqflectDaemonApp() {

  def start: ZIO[Client, Throwable, Unit] =
    for {
      _         <- ZIO.logInfo("Starting scheduler")
      iteration <- Ref.make(1)
      _         <-
        (for {
          i           <- iteration.get
          _           <- ZIO.logInfo(s"Iteration $i")
          _           <- iteration.set(i + 1)
          response    <- makeReq()
          _           <- ZIO.logInfo(s"$response")
        } yield ()).repeat(Schedule.spaced(60.seconds))
    } yield ()

  private def makeReq() = {
    val url = "https://distrowatch.com/news/dw.xml"

    Client
      .request(
        url,
        addZioUserAgentHeader = true,
      )
      .map(resp => resp.server.getOrElse("unknown"))
      .onError(e => ZIO.logErrorCause("ClientException:", e) &> ZIO.succeed("unknown"))
  }

}
