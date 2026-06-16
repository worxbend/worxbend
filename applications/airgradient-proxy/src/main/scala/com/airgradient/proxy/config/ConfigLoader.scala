package com.airgradient.proxy.config

import pureconfig.*

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

object ConfigLoader:

  given ConfigReader[EndpointMode] = ConfigReader[String].emap { s =>
    s.toLowerCase match
      case "cached"      => Right(EndpointMode.Cached)
      case "passthrough" => Right(EndpointMode.Passthrough)
      case "disabled"    => Right(EndpointMode.Disabled)
      case other         => Left(pureconfig.error.CannotConvert(other, "EndpointMode", s"unknown mode: $other"))
  }

  given ConfigReader[HttpConfig]      = ConfigReader.derived
  given ConfigReader[UpstreamConfig]  = ConfigReader.derived
  given ConfigReader[PollingConfig]   = ConfigReader.derived
  given ConfigReader[CacheConfig]     = ConfigReader.derived
  given ConfigReader[EndpointsConfig] = ConfigReader.derived
  given ConfigReader[LoggingConfig]   = ConfigReader.derived
  given ConfigReader[AppConfig]       = ConfigReader.derived

  def load(): Either[String, AppConfig] =
    ConfigSource
      .default
      .at("airgradient-proxy")
      .load[AppConfig]
      .left
      .map(_.prettyPrint())
      .flatMap(validate)

  private def validate(cfg: AppConfig): Either[String, AppConfig] =
    for
      _ <- validateUri(cfg.upstream.baseUrl)
      _ <- require(cfg.polling.interval.toMillis > 0, "polling.interval must be positive")
      _ <- require(cfg.upstream.requestTimeout.toMillis > 0, "upstream.request-timeout must be positive")
      _ <- require(
             cfg.upstream.requestTimeout.toMillis < cfg.polling.interval.toMillis,
             "upstream.request-timeout should be less than polling.interval",
           )
      _ <- require(
             cfg.cache.freshTtl.toMillis <= cfg.cache.staleTtl.toMillis,
             "cache.fresh-ttl must be <= cache.stale-ttl",
           )
      _ <- require(
             cfg.cache.staleTtl.toMillis <= cfg.cache.expiredAfter.toMillis,
             "cache.stale-ttl must be <= cache.expired-after",
           )
      _ <- require(cfg.http.port > 0 && cfg.http.port <= 65535, "http.port must be a valid port number")
    yield cfg

  private def validateUri(url: String): Either[String, Unit] =
    Try(java.net.URI.create(url)).toEither.left.map(_ => s"upstream.base-url is not a valid URI: $url").map(_ => ())

  private def require(cond: Boolean, msg: String): Either[String, Unit] =
    if cond then Right(()) else Left(msg)
