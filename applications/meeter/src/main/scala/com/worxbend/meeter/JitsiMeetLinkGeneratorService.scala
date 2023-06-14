package com.worxbend.meeter

import zio.UIO
import zio.ZIO
import zio.ZLayer

class JitsiMeetLinkGeneratorService extends MeetLinkGeneratorService:
  override def generateLink(): UIO[String] = ZIO.succeed("jitsi")
object JitsiMeetLinkGeneratorService:

  val layer: ZLayer[
    Any,
    Nothing,
    MeetLinkGeneratorService,
  ] = ZLayer.fromFunction(apply _)

  def apply() = new JitsiMeetLinkGeneratorService()
