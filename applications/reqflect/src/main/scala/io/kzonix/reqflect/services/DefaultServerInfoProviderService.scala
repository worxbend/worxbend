package io.kzonix.reqflect.services

import io.kzonix.reqflect.services.exceptions.ReqflectServiceException
import io.kzonix.reqflect.services.exceptions.ReqflectServiceException.GeneralException
import io.kzonix.reqflect.services.models.SystemInfo

import zio.Cause
import zio.IO
import zio.Task
import zio.ZIO

import scala.jdk.CollectionConverters.*

import java.lang.management.ManagementFactory
import java.lang.management.MemoryMXBean
import java.lang.management.OperatingSystemMXBean
import java.net.NetworkInterface

class DefaultServerInfoProviderService extends ServerInfoProviderService {

  override def getSystemInfo(): ZIO[Any, ReqflectServiceException, SystemInfo] =
    for {
      _   <- ZIO.logInfo("Loading from system")
      res <- fetchSystemInfo()
    } yield res

  private def fetchSystemInfo() =
    for {
      hostname          <- readEnv("HOSTNAME")
      containerName     <- readEnv("CONTAINER_NAME")
      containerImage    <- readEnv("CONTAINER_IMAGE")
      osName            <- readProp("os.name")
      osVersion         <- readProp("os.version")
      osArch            <- readProp("os.arch")
      javaVersion       <- readProp("java.version")
      networkInterfaces <- fetchNetworkInfo()
    } yield SystemInfo(
      containerId = hostname,
      containerName = containerName,
      containerImage = containerImage,
      operatingSystemName = osName,
      operatingSystemVersion = osVersion,
      operatingSystemArchitecture = osArch,
      javaVersion = javaVersion,
      networkInterfaces = networkInterfaces,
    )

  private def readEnv(name: String) =
    read(
      f = zio.System.env(name),
      default = "",
    )

  private def readProp(name: String): ZIO[Any, GeneralException, String] =
    read(
      f = zio.System.property(name),
      default = "",
    )

  private def read[V](f: => ZIO[Any, Throwable, Option[V]], default: V) =
    f.mapBoth(
      e => GeneralException(e.getMessage),
      _.getOrElse(default),
    )

  private def fetchNetworkInfo(): ZIO[Any, ReqflectServiceException, List[SystemInfo.NetworkInterface]] =
    ZIO
      .attemptBlocking {
        val interfaces: Seq[NetworkInterface] = NetworkInterface.getNetworkInterfaces.asScala.toList
        interfaces.map { interface =>
          SystemInfo.NetworkInterface(
            name = interface.getName,
            displayName = interface.getDisplayName,
            mtu = interface.getMTU,
            hardwareAddress = Option(interface.getHardwareAddress)
              .map(_.map("%02X" format _).mkString(":"))
              .getOrElse("n/a"),
            inetAddresses = interface.getInetAddresses.asScala.toList.map(_.toString),
          )
        }.toList
      }
      .tapError(e => ZIO.logErrorCause(Cause.fail(e)) &> ZIO.failCause(Cause.fail(e)))
      .mapError(e => GeneralException(e.getMessage))

}

object DefaultServerInfoProviderService:

  def make(): DefaultServerInfoProviderService = new DefaultServerInfoProviderService
