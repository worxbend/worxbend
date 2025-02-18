package com.worxbend.meeter

import zio.ZLayer

object MeeterAppModule {

  val all: ZLayer[
    Any,
    Nothing,
    Seq[MeetLinkGeneratorService],
  ] = ZLayer.collectAll(Seq(GoogleMeetLinkGeneratorService.layer, JitsiMeetLinkGeneratorService.layer))

  val res: ZLayer[
    Any,
    Nothing,
    MeetLinkGeneratorService,
  ] = all >>> CompositeMeetLinkGeneratorService.layer

  implicit final private lazy val y: 42 = 42
  implicit final private lazy val x: 42 = 42

}
