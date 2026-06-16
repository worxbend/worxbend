package com.airgradient.proxy.upstream

import com.airgradient.proxy.config.UpstreamConfig
import com.airgradient.proxy.domain.CachedSnapshot
import com.airgradient.proxy.http.AirGradientEndpoints
import sttp.client4.BackendOptions
import sttp.client4.SttpClientException
import sttp.client4.asByteArray
import sttp.client4.basicRequest
import sttp.client4.okhttp.OkHttpSyncBackend
import sttp.model.Uri
import sttp.tapir.client.sttp4.SttpClientInterpreter

import java.net.SocketTimeoutException
import java.time.Instant
import scala.util.Try
import scala.util.control.NonFatal

final class SttpAirGradientClient(config: UpstreamConfig) extends AirGradientClient:

  private val backend = OkHttpSyncBackend(
    BackendOptions.connectionTimeout(config.connectTimeout)
  )

  private val baseUri = Uri.unsafeParse(config.baseUrl)

  // Pre-built request factory derived directly from the endpoint definition.
  private val callMeasures =
    SttpClientInterpreter()
      .toRequestThrowDecodeFailures(
        AirGradientEndpoints.measuresCurrentEndpoint,
        Some(baseUri),
      )

  def fetchCurrentMeasures(): Either[UpstreamError, CachedSnapshot] =
    val start   = System.currentTimeMillis()
    val request = callMeasures(()).readTimeout(config.requestTimeout)
    Try(backend.send(request)).toEither
      .left.map(mapException)
      .flatMap { response =>
        val durationMs = System.currentTimeMillis() - start
        response.body match
          case Left(statusCode) =>
            Left(UpstreamError.BadStatus(statusCode.code, s"upstream returned ${statusCode.code}"))
          case Right((bytes, headers)) =>
            JsonValidator.validate(bytes).map { _ =>
              val contentType = headers
                .find(_.name.equalsIgnoreCase("content-type"))
                .map(_.value)
                .getOrElse("application/json")
              CachedSnapshot(
                payload                = bytes,
                contentType            = contentType,
                receivedAt             = Instant.now(),
                upstreamDurationMillis = durationMs,
                upstreamUrl            = s"${config.baseUrl}/measures/current",
                upstreamStatusCode     = response.code.code,
              )
            }
      }

  def fetchConfig(): Either[UpstreamError, Array[Byte]] =
    val request = basicRequest
      .get(baseUri.addPath("config"))
      .readTimeout(config.requestTimeout)
      .response(asByteArray)
    Try(backend.send(request)).toEither
      .left.map(mapException)
      .flatMap { response =>
        response.body match
          case Left(msg)    => Left(UpstreamError.BadStatus(response.code.code, msg))
          case Right(bytes) => Right(bytes)
      }

  def putConfig(payload: Array[Byte]): Either[UpstreamError, Array[Byte]] =
    val request = basicRequest
      .put(baseUri.addPath("config"))
      .readTimeout(config.requestTimeout)
      .contentType("application/json")
      .body(payload)
      .response(asByteArray)
    Try(backend.send(request)).toEither
      .left.map(mapException)
      .flatMap { response =>
        response.body match
          case Left(msg)    => Left(UpstreamError.BadStatus(response.code.code, msg))
          case Right(bytes) => Right(bytes)
      }

  private def mapException(e: Throwable): UpstreamError = e match
    case e: InterruptedException =>
      Thread.currentThread().interrupt()
      UpstreamError.Interrupted(s"request interrupted: ${e.getMessage}")
    case e: SttpClientException.ConnectException =>
      UpstreamError.Network(s"connection failed: ${e.getMessage}")
    case e: SttpClientException.ReadException
        if Option(e.getCause).exists(_.isInstanceOf[SocketTimeoutException]) =>
      UpstreamError.Timeout(s"request timed out: ${e.getCause.getMessage}")
    case e: SttpClientException.ReadException =>
      UpstreamError.Network(s"IO error: ${e.getMessage}")
    case NonFatal(e) =>
      UpstreamError.Unexpected(s"unexpected error: ${e.getMessage}")
