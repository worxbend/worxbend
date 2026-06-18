package io.kzonix.cetus

import zio.*
import zio.http.*
import zio.json.*
import zio.json.ast.Json

case class CetusDaemonApp() {

  def start: ZIO[
    Client,
    Throwable,
    Unit,
  ] =
    for {
      _ <- ZIO.logInfo("Starting scheduler")
      _ <-
        (for {
          text <- makeReq().flatMap(resp => resp.body.asString)
          _    <- ZIO.logInfo(s"${text.fromJson[Json].map(_.toJsonPretty)}")
        } yield ()).repeat(Schedule.spaced(60.seconds))
    } yield ()

  private def makeReq() = {
    val url = "https://httpbin.org/anything"
    Client
      .batched(Request.get(url))
      .onError(e => ZIO.succeed(Response.text(e.prettyPrint)))
  }

}
