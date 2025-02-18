package com.worxbend.meeter

import zio.{UIO, ZIO, ZLayer}

class CompositeMeetLinkGeneratorService(services: Seq[MeetLinkGeneratorService]) extends MeetLinkGeneratorService:
  override def generateLink(): UIO[String] = ZIO.foreach(services)(_.generateLink()).map(_.mkString(" and "))

object CompositeMeetLinkGeneratorService:

  val layer: ZLayer[Seq[MeetLinkGeneratorService], Nothing, MeetLinkGeneratorService] = ZLayer.fromFunction(apply)

  def apply(services: Seq[MeetLinkGeneratorService]): CompositeMeetLinkGeneratorService =
    new CompositeMeetLinkGeneratorService(services)
