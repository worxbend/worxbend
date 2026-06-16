package com.airgradient.proxy

import com.airgradient.proxy.config.*
import munit.FunSuite

import scala.concurrent.duration.*

class ConfigLoaderSuite extends FunSuite:

  test("load default config from application.conf") {
    val result = ConfigLoader.load()
    assert(result.isRight, s"Expected Right but got Left: ${result.left.toOption.getOrElse("")}")
    val cfg = result.toOption.get
    assertEquals(cfg.http.port, 8080)
    assertEquals(cfg.upstream.baseUrl, "http://airgradient_ecda3b1eaaaf.local")
    assertEquals(cfg.polling.interval, 5.seconds)
    assertEquals(cfg.cache.freshTtl, 10.seconds)
    assertEquals(cfg.cache.staleTtl, 5.minutes)
    assertEquals(cfg.cache.serveExpired, false)
    assertEquals(cfg.endpoints.configGetMode, EndpointMode.Cached)
    assertEquals(cfg.endpoints.configPutMode, EndpointMode.Disabled)
  }

  test("EndpointMode: cached parses correctly") {
    given pureconfig.ConfigReader[EndpointMode] = ConfigLoader.given_ConfigReader_EndpointMode
    val reader = summon[pureconfig.ConfigReader[EndpointMode]]
    val r      = reader.from(com.typesafe.config.ConfigValueFactory.fromAnyRef("cached"))
    assertEquals(r, Right(EndpointMode.Cached))
  }

  test("EndpointMode: passthrough parses correctly") {
    given pureconfig.ConfigReader[EndpointMode] = ConfigLoader.given_ConfigReader_EndpointMode
    val reader = summon[pureconfig.ConfigReader[EndpointMode]]
    val r      = reader.from(com.typesafe.config.ConfigValueFactory.fromAnyRef("passthrough"))
    assertEquals(r, Right(EndpointMode.Passthrough))
  }

  test("EndpointMode: disabled parses correctly") {
    given pureconfig.ConfigReader[EndpointMode] = ConfigLoader.given_ConfigReader_EndpointMode
    val reader = summon[pureconfig.ConfigReader[EndpointMode]]
    val r      = reader.from(com.typesafe.config.ConfigValueFactory.fromAnyRef("disabled"))
    assertEquals(r, Right(EndpointMode.Disabled))
  }

  test("EndpointMode: unknown value fails") {
    given pureconfig.ConfigReader[EndpointMode] = ConfigLoader.given_ConfigReader_EndpointMode
    val reader = summon[pureconfig.ConfigReader[EndpointMode]]
    val r      = reader.from(com.typesafe.config.ConfigValueFactory.fromAnyRef("unknown"))
    assert(r.isLeft)
  }
