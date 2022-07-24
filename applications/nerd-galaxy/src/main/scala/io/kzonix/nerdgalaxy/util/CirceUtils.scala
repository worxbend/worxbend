package io.kzonix.nerdgalaxy.util

object CirceUtils {

  import io.circe.generic.extras.Configuration
  import io.circe.Encoder

  val DefaultCirceConfig: Configuration = Configuration
    .default
    .withDefaults
    .withSnakeCaseMemberNames
    .withDiscriminator("kind")

  def nonNullEncoder[T](encoder: Encoder[T]): Encoder[T] =
    encoder.mapJson(json => json.dropNullValues.dropEmptyValues)

}
