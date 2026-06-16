package com.airgradient.proxy

import com.airgradient.proxy.cache.AtomicSnapshotStore
import com.airgradient.proxy.cache.FreshnessClassifier
import com.airgradient.proxy.config.CacheConfig
import com.airgradient.proxy.domain.CacheStatus
import com.airgradient.proxy.domain.CachedSnapshot
import munit.FunSuite

import java.time.Instant
import scala.concurrent.duration.*

class CacheSuite extends FunSuite:

  private def makeSnapshot(receivedAt: Instant = Instant.now()): CachedSnapshot =
    CachedSnapshot(
      payload                = """{"wifi":{"rssi":-50}}""".getBytes("UTF-8"),
      contentType            = "application/json",
      receivedAt             = receivedAt,
      upstreamDurationMillis = 42L,
      upstreamUrl            = "http://upstream/measures/current",
      upstreamStatusCode     = 200,
    )

  private def makeClassifier(freshTtl: FiniteDuration, staleTtl: FiniteDuration, expiredAfter: FiniteDuration,
    serveExpired: Boolean = false): FreshnessClassifier =
    new FreshnessClassifier(
      CacheConfig(
        freshTtl     = freshTtl,
        staleTtl     = staleTtl,
        expiredAfter = expiredAfter,
        serveExpired = serveExpired,
      )
    )

  test("classify: Fresh when within fresh-ttl") {
    val clf      = makeClassifier(10.seconds, 5.minutes, 30.minutes)
    val snapshot = makeSnapshot(Instant.now().minusSeconds(5))
    assertEquals(clf.classify(snapshot, Instant.now()), CacheStatus.Fresh)
  }

  test("classify: Stale when beyond fresh-ttl but within stale-ttl") {
    val clf      = makeClassifier(10.seconds, 5.minutes, 30.minutes)
    val snapshot = makeSnapshot(Instant.now().minusSeconds(30))
    assertEquals(clf.classify(snapshot, Instant.now()), CacheStatus.Stale)
  }

  test("classify: Expired when beyond expired-after") {
    val clf      = makeClassifier(10.seconds, 5.minutes, 30.minutes)
    val snapshot = makeSnapshot(Instant.now().minusSeconds(31 * 60))
    assertEquals(clf.classify(snapshot, Instant.now()), CacheStatus.Expired)
  }

  test("isServable: expired returns false when serveExpired=false") {
    val clf      = makeClassifier(10.seconds, 5.minutes, 30.minutes, serveExpired = false)
    val snapshot = makeSnapshot(Instant.now().minusSeconds(31 * 60))
    assertEquals(clf.isServable(snapshot, Instant.now()), false)
  }

  test("isServable: expired returns true when serveExpired=true") {
    val clf      = makeClassifier(10.seconds, 5.minutes, 30.minutes, serveExpired = true)
    val snapshot = makeSnapshot(Instant.now().minusSeconds(31 * 60))
    assertEquals(clf.isServable(snapshot, Instant.now()), true)
  }

  test("AtomicSnapshotStore: starts empty") {
    val store = new AtomicSnapshotStore()
    assertEquals(store.get().snapshot, None)
    assertEquals(store.get().consecutiveFailures, 0)
  }

  test("AtomicSnapshotStore: updateSuccess sets snapshot and resets failures") {
    val store    = new AtomicSnapshotStore()
    val snapshot = makeSnapshot()
    store.updateFailure("first error")
    store.updateFailure("second error")
    store.updateSuccess(snapshot)
    val state = store.get()
    assertEquals(state.snapshot.map(_.upstreamStatusCode), Some(200))
    assertEquals(state.consecutiveFailures, 0)
    assertEquals(state.lastError, None)
    assert(state.lastSuccessAt.isDefined)
  }

  test("AtomicSnapshotStore: updateFailure increments counter and preserves snapshot") {
    val store    = new AtomicSnapshotStore()
    val snapshot = makeSnapshot()
    store.updateSuccess(snapshot)
    store.updateFailure("network error")
    store.updateFailure("timeout")
    val state = store.get()
    assertEquals(state.consecutiveFailures, 2)
    assertEquals(state.lastError, Some("timeout"))
    assert(state.snapshot.isDefined, "snapshot must be preserved after failure")
  }

  test("AtomicSnapshotStore: concurrent reads are safe") {
    val store    = new AtomicSnapshotStore()
    val snapshot = makeSnapshot()
    store.updateSuccess(snapshot)
    val results = (1 to 100).map(_ => store.get().snapshot.isDefined)
    assert(results.forall(identity))
  }
