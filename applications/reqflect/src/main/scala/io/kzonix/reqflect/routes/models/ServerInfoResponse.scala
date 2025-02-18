package io.kzonix.reqflect.routes.models

import zio.json.DeriveJsonCodec
import zio.json.DeriveJsonEncoder
import zio.json.JsonCodec
import zio.json.JsonEncoder
import zio.json.SnakeCase
import zio.json.jsonMemberNames

import io.kzonix.reqflect.services.models.SystemInfo

@jsonMemberNames(SnakeCase)
case class ServerInfoResponse(
    containerId:                 String,
    containerName:               String,
    containerImage:              String,
    operatingSystemName:         String,
    operatingSystemVersion:      String,
    operatingSystemArchitecture: String,
    networkInterfaces:           List[NetworkInterfaceResponse],
    javaVersion:                 String,
    hostname:                    String,
    url:                         String,
    method:                      String,
    remoteAddress:               String,
    headers:                     Map[String, String],
    host:                        String,
)

object ServerInfoResponse:
  implicit val encoder: JsonCodec[ServerInfoResponse] = DeriveJsonCodec.gen[ServerInfoResponse]
