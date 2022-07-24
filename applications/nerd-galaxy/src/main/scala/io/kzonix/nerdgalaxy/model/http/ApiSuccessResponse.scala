package io.kzonix.nerdgalaxy.model.http

import io.circe.generic.extras.Configuration
import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder
import io.kzonix.nerdgalaxy.util.CirceUtils.DefaultCirceConfig

case class ApiSuccessResponse[T](
    data: T,
    warns: Option[ApiWarningMap] = None)

object ApiSuccessResponse {

  import io.kzonix.nerdgalaxy.util.CirceUtils.nonNullEncoder

  implicit val conf: Configuration = DefaultCirceConfig

  @annotation.nowarn
  implicit def ApiResponseEncoder[T: Encoder]: Encoder[ApiSuccessResponse[T]] =
    nonNullEncoder(deriveConfiguredEncoder)

  @annotation.nowarn
  implicit def ApiResponseDecoder[T: Decoder]: Decoder[ApiSuccessResponse[T]] =
    deriveConfiguredDecoder

}
