package io.kzonix.reqflect.services

import io.kzonix.reqflect.services.CacheAwareServerInfoProviderService.makeCache
import io.kzonix.reqflect.services.exceptions.ReqflectServiceException
import io.kzonix.reqflect.services.models.SystemInfo

import zio.*
import zio.cache.Cache
import zio.cache.Lookup

class CacheAwareServerInfoProviderService(cache: Cache[String, ReqflectServiceException, SystemInfo])
    extends ServerInfoProviderService:
  override def getSystemInfo(): ZIO[Any, ReqflectServiceException, SystemInfo] =
    for {
      _       <- ZIO.logInfo("Loading from cache...")
      sysInfo <- cache.get("DEFAULT_SYSTEM_INFO")
    } yield sysInfo
object CacheAwareServerInfoProviderService:

  def make(cache: Cache[String, ReqflectServiceException, SystemInfo]) = new CacheAwareServerInfoProviderService(cache)

  def makeCache(): URIO[ServerInfoProviderService, Cache[String, ReqflectServiceException, SystemInfo]] =
    for {
      service <- ZIO.service[ServerInfoProviderService]
      cache   <- Cache.make(
                   capacity = 100,
                   timeToLive = 5.minutes,
                   lookup = Lookup(_ =>
                     for {
                       _   <- ZIO.logInfo("Cache miss...")
                       res <- service.getSystemInfo()
                     } yield res),
                 )
    } yield cache
