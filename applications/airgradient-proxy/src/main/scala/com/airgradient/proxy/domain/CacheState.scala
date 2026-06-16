package com.airgradient.proxy.domain

import java.time.Instant

final case class CacheState(
  snapshot:            Option[CachedSnapshot],
  lastAttemptAt:       Option[Instant],
  lastSuccessAt:       Option[Instant],
  lastFailureAt:       Option[Instant],
  consecutiveFailures: Int,
  lastError:           Option[String],
)

object CacheState:
  val empty: CacheState = CacheState(
    snapshot            = None,
    lastAttemptAt       = None,
    lastSuccessAt       = None,
    lastFailureAt       = None,
    consecutiveFailures = 0,
    lastError           = None,
  )
