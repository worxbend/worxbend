package com.airgradient.proxy.config

import scala.concurrent.duration.FiniteDuration

final case class HttpConfig(
  host: String,
  port: Int,
)

final case class UpstreamConfig(
  baseUrl:        String,
  connectTimeout: FiniteDuration,
  requestTimeout: FiniteDuration,
)

final case class PollingConfig(
  interval:     FiniteDuration,
  initialFetch: Boolean,
  jitter:       FiniteDuration,
  maxBackoff:   FiniteDuration,
)

final case class CacheConfig(
  freshTtl:     FiniteDuration,
  staleTtl:     FiniteDuration,
  expiredAfter: FiniteDuration,
  serveExpired: Boolean,
)

enum EndpointMode:
  case Cached
  case Passthrough
  case Disabled

final case class EndpointsConfig(
  configGetMode: EndpointMode,
  configPutMode: EndpointMode,
)

final case class LoggingConfig(
  json:  Boolean,
  level: String,
)

final case class AppConfig(
  http:      HttpConfig,
  upstream:  UpstreamConfig,
  polling:   PollingConfig,
  cache:     CacheConfig,
  endpoints: EndpointsConfig,
  logging:   LoggingConfig,
)
