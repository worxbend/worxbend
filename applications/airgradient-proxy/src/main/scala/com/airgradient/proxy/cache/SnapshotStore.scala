package com.airgradient.proxy.cache

import com.airgradient.proxy.domain.CachedSnapshot
import com.airgradient.proxy.domain.CacheState

trait SnapshotStore:

  def get(): CacheState
  def updateSuccess(snapshot: CachedSnapshot): Unit
  def updateFailure(error: String): Unit
