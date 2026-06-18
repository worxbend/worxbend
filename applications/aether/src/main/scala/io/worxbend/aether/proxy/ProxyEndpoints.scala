package io.worxbend.aether.proxy

import io.worxbend.aether.AetherConfig
import io.worxbend.aether.MeasuresCache
import io.worxbend.aether.domain.AirGradientMeasures
import io.worxbend.aether.domain.HealthResponse

import cats.Id

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.bundle.SwaggerInterpreter

class ProxyEndpoints(cache: MeasuresCache, config: AetherConfig):

  private val measuresEndpoint: PublicEndpoint[Unit, (StatusCode, String), AirGradientMeasures, Any] =
    endpoint.get
      .in("api" / "v1" / "measures" / "current")
      .description("Current air quality measurements proxied from the AirGradient device")
      .out(jsonBody[AirGradientMeasures])
      .errorOut(statusCode.and(stringBody))

  private val healthEndpoint: PublicEndpoint[Unit, Unit, HealthResponse, Any] =
    endpoint.get
      .in("api" / "v1" / "health")
      .description("Proxy liveness and upstream connectivity check")
      .out(jsonBody[HealthResponse])

  private val measuresServerEndpoint =
    measuresEndpoint.serverLogic[Id] { _ =>
      cache.get match
        case Some(m) => Right(m)
        case None    => Left(StatusCode.ServiceUnavailable -> "No measurements yet — first poll cycle not complete")
    }

  private val healthServerEndpoint =
    healthEndpoint.serverLogicSuccess[Id] { _ =>
      HealthResponse(
        status = "ok",
        upstreamUrl = config.airgradient.baseUrl,
        hasMeasures = cache.get.isDefined,
      )
    }

  private val serverEndpoints: List[ServerEndpoint[Any, Id]] =
    List(measuresServerEndpoint, healthServerEndpoint)

  private val swaggerEndpoints: List[ServerEndpoint[Any, Id]] =
    SwaggerInterpreter().fromServerEndpoints[Id](serverEndpoints, "Aether — AirGradient Proxy", "1.0.0")

  val allEndpoints: List[ServerEndpoint[Any, Id]] =
    serverEndpoints ++ swaggerEndpoints
