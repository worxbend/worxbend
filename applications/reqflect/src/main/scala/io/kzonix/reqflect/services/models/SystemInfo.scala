package io.kzonix.reqflect.services.models

import io.kzonix.reqflect.services.models.SystemInfo.NetworkInterface

case class SystemInfo(
    containerId:                 String,
    containerName:               String,
    containerImage:              String,
    operatingSystemName:         String,
    operatingSystemVersion:      String,
    operatingSystemArchitecture: String,
    networkInterfaces:           List[NetworkInterface],
    javaVersion:                 String,
)

object SystemInfo:

  case class NetworkInterface(
      name:            String,
      displayName:     String,
      hardwareAddress: String,
      inetAddresses:   List[String],
      mtu:             Int,
  )
