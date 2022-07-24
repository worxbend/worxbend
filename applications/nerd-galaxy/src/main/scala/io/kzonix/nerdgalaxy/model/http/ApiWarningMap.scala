package io.kzonix.nerdgalaxy.model.http

import io.circe.generic.extras.ConfiguredJsonCodec
import io.circe.generic.extras.Configuration
import io.kzonix.nerdgalaxy.util.CirceUtils.DefaultCirceConfig

@ConfiguredJsonCodec
case class ApiWarningMap(
    data: List[Warning] = List.empty)

object ApiWarningMap {
  implicit val conf: Configuration = DefaultCirceConfig
}
