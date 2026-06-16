package com.airgradient.proxy.domain

import java.time.Instant

final case class CachedSnapshot(
  payload:               Array[Byte],
  contentType:           String,
  receivedAt:            Instant,
  upstreamDurationMillis: Long,
  upstreamUrl:           String,
  upstreamStatusCode:    Int,
)
