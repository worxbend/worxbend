package io.worxbend.aether

import pureconfig.ConfigReader
import pureconfig.ConfigSource
import pureconfig.generic.derivation.default.*

case class ServerConfig(
    host: String = "0.0.0.0",
    port: Int = 8080,
) derives ConfigReader

case class AirGradientConfig(
    baseUrl: String,
    pollIntervalMs: Long = 30_000L,
    timeoutMs: Int = 5_000,
) derives ConfigReader

case class AetherConfig(
    server: ServerConfig = ServerConfig(),
    airgradient: AirGradientConfig,
) derives ConfigReader

object AetherConfig:
  def load(): AetherConfig = ConfigSource.default.loadOrThrow[AetherConfig]
