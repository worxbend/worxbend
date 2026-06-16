package io.worxbend.aether

import io.worxbend.aether.domain.AirGradientMeasures

import scala.concurrent.duration.DurationInt

import sttp.client4.DefaultSyncBackend
import sttp.client4.UriContext
import sttp.client4.basicRequest
import sttp.client4.circe.asJson

class AirGradientClient(config: AirGradientConfig):
  private val backend = DefaultSyncBackend()

  def fetchMeasures(): Either[String, AirGradientMeasures] =
    val response = basicRequest
      .get(uri"${config.baseUrl}/measures/current")
      .readTimeout(config.timeoutMs.millis)
      .response(asJson[AirGradientMeasures])
      .send(backend)
    response.body.left.map(_.getMessage)

  def close(): Unit = backend.close()
