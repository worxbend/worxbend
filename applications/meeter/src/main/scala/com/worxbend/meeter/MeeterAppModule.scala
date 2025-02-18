package com.worxbend.meeter

import zio.ZLayer

import scala.collection.immutable.Seq

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

  
  

}
