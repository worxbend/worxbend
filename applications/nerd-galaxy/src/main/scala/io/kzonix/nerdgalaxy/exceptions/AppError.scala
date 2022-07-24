package io.kzonix.nerdgalaxy.exceptions

import io.circe.Json
import io.kzonix.nerdgalaxy.exceptions.AppError.ErrorDef

case class AppError(
    definition: ErrorDef,
    timestamp: Long,
    details: Map[String, Json])
    extends AppRuntimeError {

  override def code: String = definition.code

  override def module: String = definition.module

  override def message: String = definition.message

}

object AppError {

  def apply(
      definition: ErrorDef,
      details: Map[String, String] = Map.empty,
      timestamp: Long = System.currentTimeMillis(),
    ): AppRuntimeError = AppError(
    definition,
    timestamp,
    details.transform((_, v) => Json.fromString(v)),
  )

  trait ErrorDef {

    def code: String
    def module: String
    def message: String

  }

  case class BaseErrorDef(
      code: String,
      module: String,
      message: String)
      extends ErrorDef

}
