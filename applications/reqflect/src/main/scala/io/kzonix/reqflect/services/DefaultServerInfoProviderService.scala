package io.kzonix.reqflect.services

import io.kzonix.reqflect.services.exceptions.ReqflectServiceException
import io.kzonix.reqflect.services.exceptions.ReqflectServiceException.GeneralException
import io.kzonix.reqflect.services.models.SystemInfo
import zio.{Cause, IO, Task, ZIO}

import java.lang.management.{ManagementFactory, MemoryMXBean, OperatingSystemMXBean}
import java.net.NetworkInterface
import scala.jdk.CollectionConverters.*

class DefaultServerInfoProviderService extends ServerInfoProviderService {

  override def getSystemInfo(): ZIO[Any, ReqflectServiceException, SystemInfo] =
    for {
      _ <- ZIO.log("Fetching the system information")
      networkInterfaces <- fetchNetworkInfo()
      _ <- ZIO.log("Fetching the system information")
    } yield SystemInfo(
      containerId = getOrUnknown(System.getenv("HOSTNAME")),
      containerName = getOrUnknown(System.getenv("CONTAINER_NAME")),
      containerImage = getOrUnknown(System.getenv("CONTAINER_IMAGE")),
      containerIp = getOrUnknown(System.getenv("DOCKER_HOST_IP")),
      operatingSystemName = getOrUnknown(System.getProperty("os.name")),
      operatingSystemVersion = getOrUnknown(System.getProperty("os.version")),
      operatingSystemArchitecture = getOrUnknown(System.getProperty("os.arch")),
      javaVersion = getOrUnknown(System.getProperty("java.version")),
      networkInterfaces = networkInterfaces,
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
            hardwareAddress = interface.getHardwareAddress.map("%02X" format _).mkString(":"),
            inetAddresses = interface.getInetAddresses.asScala.toList.map(_.toString),
          )
        }.toList
      }
      .tapError(e => ZIO.logErrorCause(Cause.fail(e)) &> ZIO.failCause(Cause.fail(e)))
      .mapError { e => GeneralException(e.getMessage) }

  private def getOrUnknown(value: => String) = Option(value).getOrElse("unknown")

}

object DefaultServerInfoProviderService:
  def make(): ServerInfoProviderService = new DefaultServerInfoProviderService
