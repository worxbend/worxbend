package io.kzonix.reqflect

import zio.*
import zio.http.*
import zio.metrics.*

import io.kzonix.reqflect.services.ServerInfoProviderService

class ReqflectDaemonApp(serverInfoProviderService: ServerInfoProviderService) {

  def start: ZIO[
    Client,
    Throwable,
    Unit,
  ] =
    for {
      _ <- ZIO.logInfo("Starting scheduler")
      _ <-
        (for {
          _          <- ZIO.logInfo(s"Scheduled operation...")
          response   <- makeReq()
          serverInfo <- serverInfoProviderService.getSystemInfo().either
          _          <- ZIO.logInfo(s"$response")
          _          <- ZIO.logInfo(s"$serverInfo")
        } yield ()).repeat(Schedule.spaced(5.seconds))
    } yield ()

  private def makeReq() = {
    val url = "https://distrowatch.com/news/dw.xml"

    Client
      .request(
        url,
        addZioUserAgentHeader = true,
      )
      .map(resp => resp.server.getOrElse("unknown"))
      .onError(e =>
        ZIO.logErrorCause(
          s"ClientException:${e.prettyPrint}",
          e,
        ) &> ZIO.succeed("unknown")
      )
  }

}

object ReqflectDaemonApp:

  def make(serverInfoProviderService: ServerInfoProviderService) =
    new ReqflectDaemonApp(serverInfoProviderService: ServerInfoProviderService)
