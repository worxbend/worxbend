package io.kzonix.nerdgalaxy.model.games

import io.circe.generic.extras.Configuration
import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder
import io.kzonix.nerdgalaxy.util.CirceUtils.DefaultCirceConfig
import io.kzonix.nerdgalaxy.util.CirceUtils.nonNullEncoder
import sttp.tapir.generic.{ Configuration => SchemaConfiguration }
import sttp.tapir.generic.Derived
import sttp.tapir._
import sttp.tapir.generic.auto._

case class Game(
    id: String,
    name: String,
    createdAt: Option[Long] = None)

object Game {

  implicit lazy val schemaConf: SchemaConfiguration = SchemaConfiguration.default.withSnakeCaseMemberNames
  implicit lazy val gamesSchema: Schema[Game]       = implicitly[Derived[Schema[Game]]].value
  implicit val conf: Configuration                  = DefaultCirceConfig
  implicit val encoder: Encoder[Game]               = nonNullEncoder[Game](deriveConfiguredEncoder[Game])
  implicit val decoder: Decoder[Game]               = deriveConfiguredDecoder[Game]

}
