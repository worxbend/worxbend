package io.kzonix.cetus.routes.models

import zio.json.DeriveJsonEncoder
import zio.json.JsonEncoder
import zio.json.SnakeCase
import zio.json.jsonMemberNames

@jsonMemberNames(SnakeCase)
case class ServerInfoResponse(
    id:         String,
    hostname:   String,
    url:        String,
    method:     String,
    ip:         List[String],
    headers:    Map[String, String],
    host:       String,
    remoteAddr: String,
  )

object ServerInfoResponse:
  implicit val encoder: JsonEncoder[ServerInfoResponse] = DeriveJsonEncoder.gen[ServerInfoResponse]
