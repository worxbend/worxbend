package com.airgradient.proxy.cache

import com.airgradient.proxy.domain.CacheState
import com.airgradient.proxy.domain.CachedSnapshot

import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

final class AtomicSnapshotStore extends SnapshotStore:

  private val state: AtomicReference[CacheState] = new AtomicReference(CacheState.empty)

  def get(): CacheState = state.get()

  def updateSuccess(snapshot: CachedSnapshot): Unit =
    val now = Instant.now()
    state.updateAndGet { prev =>
      prev.copy(
        snapshot            = Some(snapshot),
        lastAttemptAt       = Some(now),
        lastSuccessAt       = Some(now),
        consecutiveFailures = 0,
        lastError           = None,
      )
    }
    ()

  def updateFailure(error: String): Unit =
    val now = Instant.now()
    state.updateAndGet { prev =>
      prev.copy(
        lastAttemptAt       = Some(now),
        lastFailureAt       = Some(now),
        consecutiveFailures = prev.consecutiveFailures + 1,
        lastError           = Some(error),
      )
    }
    ()
