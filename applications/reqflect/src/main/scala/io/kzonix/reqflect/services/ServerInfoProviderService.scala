package io.kzonix.reqflect.services

import io.kzonix.reqflect.services.exceptions.ReqflectServiceException
import io.kzonix.reqflect.services.models.SystemInfo

import zio.ZIO

trait ServerInfoProviderService {
  def getSystemInfo(): ZIO[
    Any,
    ReqflectServiceException,
    SystemInfo,
  ]
}

object ServerInfoProviderService:
  def getSystemInfo(): ZIO[
    ServerInfoProviderService,
    ReqflectServiceException,
    SystemInfo,
  ] = ZIO.serviceWithZIO[ServerInfoProviderService](_.getSystemInfo())
