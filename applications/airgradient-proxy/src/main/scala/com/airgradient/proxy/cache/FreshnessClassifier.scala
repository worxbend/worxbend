package com.airgradient.proxy.cache

import com.airgradient.proxy.config.CacheConfig
import com.airgradient.proxy.domain.CacheStatus
import com.airgradient.proxy.domain.CachedSnapshot

import java.time.Instant

final class FreshnessClassifier(cacheConfig: CacheConfig):

  def classify(snapshot: CachedSnapshot, now: Instant): CacheStatus =
    val ageMillis      = now.toEpochMilli - snapshot.receivedAt.toEpochMilli
    val freshTtlMs     = cacheConfig.freshTtl.toMillis
    val staleTtlMs     = cacheConfig.staleTtl.toMillis
    val expiredAfterMs = cacheConfig.expiredAfter.toMillis

    if ageMillis > expiredAfterMs then CacheStatus.Expired
    else if ageMillis > staleTtlMs then CacheStatus.Stale
    else if ageMillis > freshTtlMs then CacheStatus.Stale
    else CacheStatus.Fresh

  def cacheAgeMillis(snapshot: CachedSnapshot, now: Instant): Long =
    now.toEpochMilli - snapshot.receivedAt.toEpochMilli

  def isServable(snapshot: CachedSnapshot, now: Instant): Boolean =
    val status = classify(snapshot, now)
    status match
      case CacheStatus.Expired => cacheConfig.serveExpired
      case CacheStatus.Empty   => false
      case _                   => true
