package io.kzonix.nerdgalaxy

import com.typesafe.scalalogging.LazyLogging
import io.kzonix.nerdgalaxy.config.HttpConfig
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import io.kzonix.nerdgalaxy.model.http.ApiErrorResponse

class RouterComponents(httpConfig: HttpConfig) extends LazyLogging {

  val baseEndpoint: Endpoint[Unit, Unit, (StatusCode, ApiErrorResponse), Unit, Any] = endpoint
    .in("api" / s"v${ httpConfig.apiVersion }")
    .errorOut(statusCode.and(jsonBody[ApiErrorResponse]))

  val publicEndpoint: Endpoint[Unit, Unit, (StatusCode, ApiErrorResponse), Unit, Any] = baseEndpoint
    .in("public")

}
