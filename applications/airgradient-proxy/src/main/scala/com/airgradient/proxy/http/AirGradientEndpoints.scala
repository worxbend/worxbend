package com.airgradient.proxy.http

import sttp.model.Header
import sttp.model.StatusCode
import sttp.tapir.*

object AirGradientEndpoints:

  // Raw byte body — Content-Type is set to application/json via the handler's response headers.
  val measuresCurrentEndpoint: PublicEndpoint[Unit, StatusCode, (Array[Byte], List[Header]), Any] =
    endpoint
      .get
      .in("measures" / "current")
      .out(byteArrayBody.and(headers))
      .errorOut(statusCode)
