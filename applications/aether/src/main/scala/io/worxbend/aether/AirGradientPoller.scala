package io.worxbend.aether

import org.slf4j.LoggerFactory

class AirGradientPoller(
    client: AirGradientClient,
    cache: MeasuresCache,
    pollIntervalMs: Long,
):
  private val logger = LoggerFactory.getLogger(getClass)

  def run(): Unit =
    logger.info(s"Poller started (interval: ${pollIntervalMs}ms)")
    while true do
      client.fetchMeasures() match
        case Right(m) =>
          cache.update(m)
          logger.debug(s"Measures refreshed — CO₂: ${m.rco2.getOrElse("n/a")} ppm, PM2.5: ${m.pm02.getOrElse("n/a")} µg/m³")
        case Left(err) =>
          logger.warn(s"Failed to fetch measures from AirGradient: $err")
      Thread.sleep(pollIntervalMs)
