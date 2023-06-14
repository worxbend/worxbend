package com.worxbend.meeter

import zio.UIO
import zio.ZIO
import zio.ZLayer

class GoogleMeetLinkGeneratorService extends MeetLinkGeneratorService:
  override def generateLink(): UIO[String] = ZIO.succeed("google-meet")
object GoogleMeetLinkGeneratorService:

  val layer: ZLayer[
    Any,
    Nothing,
    MeetLinkGeneratorService,
  ] = ZLayer.fromFunction(apply _)

  def apply() = new GoogleMeetLinkGeneratorService()
