package io.kzonix.nerdgalaxy.model.http

import io.circe.generic.extras.ConfiguredJsonCodec
import io.circe.generic.extras.Configuration
import io.kzonix.nerdgalaxy.util.CirceUtils.DefaultCirceConfig

@ConfiguredJsonCodec
case class Warning(
    name: String,
    msg: String,
    details: Map[String, String] = Map.empty)

object Warning {
  implicit val conf: Configuration = DefaultCirceConfig
}
