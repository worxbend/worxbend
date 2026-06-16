package com.airgradient.proxy

import com.airgradient.proxy.cache.AtomicSnapshotStore
import com.airgradient.proxy.config.*
import com.airgradient.proxy.upstream.SttpAirGradientClient
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import munit.FunSuite

import scala.concurrent.duration.*

class IntegrationSuite extends FunSuite:

  private val samplePayload =
    """{"wifi":{"rssi":-72},"pm01":1.1,"pm02":1.5,"pm10":1.6,"pm003Count":120,"atmp":22.5,"rhum":55}"""

  private var wireMock: WireMockServer = null

  override def beforeAll(): Unit =
    wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort())
    wireMock.start()

  override def afterAll(): Unit =
    if wireMock != null then wireMock.stop()

  private def makeClient(port: Int): SttpAirGradientClient =
    new SttpAirGradientClient(
      UpstreamConfig(
        baseUrl        = s"http://localhost:$port",
        connectTimeout = 500.millis,
        requestTimeout = 1.second,
      )
    )

  test("fetchCurrentMeasures: returns snapshot for valid 200 JSON response") {
    wireMock.stubFor(
      get(urlEqualTo("/measures/current"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(samplePayload)
        )
    )

    val client = makeClient(wireMock.port())
    val result = client.fetchCurrentMeasures()

    assert(result.isRight, s"Expected Right but got: $result")
    val snapshot = result.toOption.get
    assertEquals(snapshot.upstreamStatusCode, 200)
    assertEquals(new String(snapshot.payload, "UTF-8"), samplePayload)
    assert(snapshot.upstreamDurationMillis >= 0L)
    assert(snapshot.receivedAt != null)
  }

  test("fetchCurrentMeasures: returns BadStatus for 5xx response") {
    wireMock.stubFor(
      get(urlEqualTo("/measures/current"))
        .willReturn(aResponse().withStatus(500).withBody("internal error"))
    )

    val result = makeClient(wireMock.port()).fetchCurrentMeasures()
    assert(result.isLeft)
    result match
      case Left(e) => assertEquals(e.errorType, "BadStatus")
      case _       => fail("expected Left")
  }

  test("fetchCurrentMeasures: returns InvalidJson for malformed response") {
    wireMock.stubFor(
      get(urlEqualTo("/measures/current"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("not json!")
        )
    )

    val result = makeClient(wireMock.port()).fetchCurrentMeasures()
    assert(result.isLeft)
    result match
      case Left(e) => assertEquals(e.errorType, "InvalidJson")
      case _       => fail("expected Left")
  }

  test("fetchCurrentMeasures: malformed JSON does not replace existing cache") {
    val store  = new AtomicSnapshotStore()
    val client = makeClient(wireMock.port())

    wireMock.stubFor(
      get(urlEqualTo("/measures/current"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(samplePayload)
        )
    )
    client.fetchCurrentMeasures() match
      case Right(s) => store.updateSuccess(s)
      case Left(e)  => fail(s"Unexpected error: $e")

    val previousPayload = new String(store.get().snapshot.get.payload, "UTF-8")

    wireMock.stubFor(
      get(urlEqualTo("/measures/current"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{bad json")
        )
    )
    client.fetchCurrentMeasures() match
      case Left(e)  => store.updateFailure(e.message)
      case Right(s) => store.updateSuccess(s)

    assertEquals(new String(store.get().snapshot.get.payload, "UTF-8"), previousPayload)
  }

  test("upstream down + empty cache: store returns no snapshot") {
    val store = new AtomicSnapshotStore()
    store.updateFailure("connection refused")
    assertEquals(store.get().snapshot, None)
    assertEquals(store.get().consecutiveFailures, 1)
  }

  test("upstream down + existing cache: snapshot is preserved") {
    val store  = new AtomicSnapshotStore()
    val client = makeClient(wireMock.port())

    wireMock.stubFor(
      get(urlEqualTo("/measures/current"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(samplePayload)
        )
    )
    client.fetchCurrentMeasures().foreach(store.updateSuccess)
    assert(store.get().snapshot.isDefined)

    store.updateFailure("network gone")
    assert(store.get().snapshot.isDefined, "snapshot must survive failure")
  }

  test("unknown JSON fields are preserved in response body") {
    val jsonWithUnknown = """{"pm01":1.0,"firmware_version_v99":"3.1.4","nested":{"x":1}}"""
    wireMock.stubFor(
      get(urlEqualTo("/measures/current"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(jsonWithUnknown)
        )
    )

    val result = makeClient(wireMock.port()).fetchCurrentMeasures()
    assert(result.isRight)
    assertEquals(new String(result.toOption.get.payload, "UTF-8"), jsonWithUnknown)
  }

  test("multiple client fetches do not increase upstream call count beyond poll count") {
    wireMock.resetRequests()
    wireMock.stubFor(
      get(urlEqualTo("/measures/current"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(samplePayload)
        )
    )

    val store  = new AtomicSnapshotStore()
    val client = makeClient(wireMock.port())

    client.fetchCurrentMeasures().foreach(store.updateSuccess)

    for _ <- 1 to 100 do
      assert(store.get().snapshot.isDefined)

    wireMock.verify(1, getRequestedFor(urlEqualTo("/measures/current")))
  }
