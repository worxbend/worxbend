package io.kzonix.nerdgalaxy.exceptions

import scala.util.control.NoStackTrace
import io.circe.Json

// TODO: Add scaladoc to describe the purpose of the custom error classes
trait AppRuntimeError extends NoStackTrace {

  def timestamp: Long
  def code: String
  def module: String
  def message: String
  def details: Map[String, Json]

}
