package io.worxbend.aether

import io.worxbend.aether.proxy.ProxyEndpoints

import ox.forkDiscard
import ox.supervised

import org.slf4j.LoggerFactory

import sttp.tapir.server.netty.sync.NettySyncServer

object AetherApp:
  private val logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit =
    val config = AetherConfig.load()
    logger.info(s"Starting Aether — AirGradient proxy → ${config.airgradient.baseUrl}")

    val client   = AirGradientClient(config.airgradient)
    val cache    = MeasuresCache()
    val poller   = AirGradientPoller(client, cache, config.airgradient.pollIntervalMs)
    val endpoints = ProxyEndpoints(cache, config)

    supervised:
      forkDiscard:
        poller.run()

      NettySyncServer()
        .host(config.server.host)
        .port(config.server.port)
        .addRoutes(endpoints.allRoutes)
        .startAndWait()

      logger.info("Server stopped")
      client.close()
