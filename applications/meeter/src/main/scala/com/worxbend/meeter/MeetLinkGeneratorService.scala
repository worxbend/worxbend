package com.worxbend.meeter

import zio.UIO
import zio.ZIO

trait MeetLinkGeneratorService:
  def generateLink(): UIO[String]
object MeetLinkGeneratorService:
  def generateLink(): ZIO[
    MeetLinkGeneratorService,
    Nothing,
    UIO[String],
  ] =
    ZIO.serviceWith[MeetLinkGeneratorService] { service =>
      service.generateLink()
    }
