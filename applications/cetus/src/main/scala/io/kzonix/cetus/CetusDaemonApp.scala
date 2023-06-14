package io.kzonix.cetus

import zio.*
import zio.http.*
import zio.json.*
import zio.json.ast.Json
import zio.logging.*
import zio.metrics.*
import zio.metrics.Metric.Counter
import zio.metrics.connectors.MetricsConfig
import zio.metrics.connectors.prometheus.*
import zio.metrics.connectors.prometheus.PrometheusPublisher
import zio.metrics.connectors.prometheus.prometheusLayer
import zio.metrics.connectors.prometheus.publisherLayer
import zio.metrics.jvm.DefaultJvmMetrics

import scala.util.Try

import java.time.temporal.ChronoUnit

case class CetusDaemonApp() {

  def start: ZIO[
    Client,
    Throwable,
    Unit,
  ] =
    for {
      _ <- ZIO.logInfo("Starting scheduler")
      _ <-
        (for {
          text <- makeReq().flatMap(resp => resp.body.asString)
          _    <- ZIO.logInfo(s"${ text.fromJson[Json].map(_.toJsonPretty) }")
        } yield ()).repeat(Schedule.spaced(60.seconds))
    } yield ()

  private def makeReq() = {
    val url = "https://httpbin.org/anything"
    Client
      .request(
        url
      )
      .onError(e => ZIO.succeed(Response.text(e.prettyPrint)))
  }

}
