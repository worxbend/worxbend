package com.airgradient.proxy.http

import com.airgradient.proxy.domain.AirGradientMeasures
import com.airgradient.proxy.domain.ProxyStatus
import io.circe.Printer
import io.circe.syntax.EncoderOps
import sttp.apispec.circe.*
import sttp.tapir.Schema
import sttp.tapir.docs.apispec.schema.TapirSchemaToJsonSchema
import sttp.tapir.generic.auto.*

// Pre-computes JSON Schema strings for every documented case class at object init.
// Ordered so that GET /_proxy/schema returns a stable name list.
object SchemaRegistry:

  private val catalog: List[(String, String)] = List(
    "AirGradientMeasures" -> render[AirGradientMeasures],
    "ProxyStatus"         -> render[ProxyStatus],
  )

  val names: List[String]         = catalog.map(_._1)
  val byName: Map[String, String] = catalog.toMap

  private def render[A: Schema]: String =
    val jsonSchema = TapirSchemaToJsonSchema(summon[Schema[A]], markOptionsAsNullable = true)
    Printer.spaces2.print(jsonSchema.asJson.deepDropNullValues)
