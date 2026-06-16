package com.airgradient.proxy.metrics

import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.core.metrics.Gauge
import io.prometheus.metrics.expositionformats.ExpositionFormats
import io.prometheus.metrics.model.registry.PrometheusRegistry
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.server.metrics.EndpointMetric
import sttp.tapir.server.metrics.Metric

import java.io.ByteArrayOutputStream

final class ProxyMetrics:

  private type Id[A] = A

  private val cacheStatusCounter: Counter =
    Counter
      .builder()
      .name("airgradient_proxy_cache_responses_total")
      .help("Responses served, labelled by cache status")
      .labelNames("status")
      .register()

  private val upstreamPollCounter: Counter =
    Counter
      .builder()
      .name("airgradient_proxy_upstream_polls_total")
      .help("Upstream poll attempts, labelled by result (success|failure)")
      .labelNames("result")
      .register()

  private val consecutiveFailuresGauge: Gauge =
    Gauge
      .builder()
      .name("airgradient_proxy_consecutive_failures")
      .help("Current count of consecutive upstream poll failures")
      .register()

  private val cacheStatusMetric: Metric[Id, Counter] =
    Metric[Id, Counter](
      metric = cacheStatusCounter,
      onRequest = (_, counter, _) =>
        EndpointMetric[Id]().onResponseBody { (_, res) =>
          val status = res.headers
            .find(_.name.equalsIgnoreCase("X-AirGradient-Proxy-Cache-Status"))
            .map(_.value)
            .getOrElse("none")
          counter.labelValues(status).inc()
        },
    )

  val tapirMetrics: PrometheusMetrics[Id] =
    PrometheusMetrics
      .default[Id]("airgradient_proxy")
      .addCustom(cacheStatusMetric)

  def recordPollSuccess(): Unit =
    upstreamPollCounter.labelValues("success").inc()

  def recordPollFailure(): Unit =
    upstreamPollCounter.labelValues("failure").inc()

  def updateConsecutiveFailures(n: Int): Unit =
    consecutiveFailuresGauge.set(n.toDouble)

  def scrapeText(): String =
    val out = new ByteArrayOutputStream()
    ExpositionFormats
      .init()
      .getPrometheusTextFormatWriter()
      .write(out, PrometheusRegistry.defaultRegistry.scrape())
    out.toString("UTF-8")
