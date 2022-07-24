package io.kzonix.nerdgalaxy.model.http

import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder
import io.kzonix.nerdgalaxy.util.CirceUtils.nonNullEncoder
import io.kzonix.nerdgalaxy.util.CirceUtils.DefaultCirceConfig
import sttp.tapir.generic.{ Configuration => SchemaConfiguration }
import sttp.tapir.generic.Derived
import sttp.tapir._
import sttp.tapir.generic.auto._

case class ApiError(
    module: String,
    code: String,
    msg: String,
    details: Map[String, String] = Map.empty,
    typedDetails: Json = Json.obj())

object ApiError {

  implicit lazy val schemaConf: SchemaConfiguration = SchemaConfiguration.default.withSnakeCaseMemberNames
  implicit lazy val jsonSchema: Schema[Json]        = Schema(SchemaType.SProduct(List.empty))
  implicit lazy val gamesSchema: Schema[ApiError]   = implicitly[Derived[Schema[ApiError]]].value
  implicit val conf: Configuration                  = DefaultCirceConfig
  implicit val encoder: Encoder[ApiError]           = nonNullEncoder[ApiError](deriveConfiguredEncoder[ApiError])
  implicit val decoder: Decoder[ApiError]           = deriveConfiguredDecoder[ApiError]

}
