package com.worxbend.meeter

import zio.{UIO, ZIO, ZLayer}

class GoogleMeetLinkGeneratorService extends MeetLinkGeneratorService:
  override def generateLink(): UIO[String] = ZIO.succeed("google-meet")

object GoogleMeetLinkGeneratorService:

  val layer: ZLayer[
    Any,
    Nothing,
    MeetLinkGeneratorService,
  ] = ZLayer.fromFunction(() => apply())

  def apply(): GoogleMeetLinkGeneratorService = new GoogleMeetLinkGeneratorService()
