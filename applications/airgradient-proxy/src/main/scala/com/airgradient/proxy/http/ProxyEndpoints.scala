package com.airgradient.proxy.http

import com.airgradient.proxy.domain.ProxyStatus
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.jsoniter.*

object ProxyEndpoints:

  private given JsonValueCodec[List[String]] = JsonCodecMaker.make

  val healthEndpoint: PublicEndpoint[Unit, Unit, Unit, Any] =
    endpoint.get.in("_proxy" / "health").out(statusCode(StatusCode.Ok))

  val readyEndpoint: PublicEndpoint[Unit, StatusCode, Unit, Any] =
    endpoint.get.in("_proxy" / "ready").errorOut(statusCode)

  val statusEndpoint: PublicEndpoint[Unit, Unit, ProxyStatus, Any] =
    endpoint.get.in("_proxy" / "status").out(jsonBody[ProxyStatus])

  val metricsEndpoint: PublicEndpoint[Unit, Unit, String, Any] =
    endpoint.get.in("_proxy" / "metrics").out(stringBody)

  val schemaListEndpoint: PublicEndpoint[Unit, Unit, List[String], Any] =
    endpoint.get
      .in("_proxy" / "schema")
      .out(jsonBody[List[String]])
      .description("Lists the names of all documented case classes with JSON Schema available")

  val schemaByNameEndpoint: PublicEndpoint[String, StatusCode, String, Any] =
    endpoint.get
      .in("_proxy" / "schema" / path[String]("typeName"))
      .out(stringBody)
      .errorOut(statusCode)
      .description("Returns the JSON Schema (Draft-04) for the named type, or 404 if unknown")
