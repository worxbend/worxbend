package com.airgradient.proxy.domain

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

final case class ProxyStatus(
  upstreamUrl:           String,
  lastAttemptAt:         Option[String],
  lastSuccessAt:         Option[String],
  lastFailureAt:         Option[String],
  consecutiveFailures:   Int,
  lastError:             Option[String],
  cacheAgeMillis:        Option[Long],
  cacheStatus:           String,
  pollingIntervalMillis: Long,
  requestTimeoutMillis:  Long,
)

object ProxyStatus:
  given codec: JsonValueCodec[ProxyStatus] = JsonCodecMaker.make
