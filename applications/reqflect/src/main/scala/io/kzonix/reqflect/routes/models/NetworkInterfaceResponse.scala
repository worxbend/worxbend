package io.kzonix.reqflect.routes.models

import zio.json.DeriveJsonCodec
import zio.json.DeriveJsonEncoder
import zio.json.JsonCodec
import zio.json.JsonEncoder
import zio.json.SnakeCase
import zio.json.jsonMemberNames

@jsonMemberNames(SnakeCase)
case class NetworkInterfaceResponse(
    name:            String,
    displayName:     String,
    hardwareAddress: String,
    inetAddresses:   List[String],
    mtu:             Int,
  )

object NetworkInterfaceResponse:

  implicit val encoder: JsonCodec[NetworkInterfaceResponse] = DeriveJsonCodec.gen[NetworkInterfaceResponse]
