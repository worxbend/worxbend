package io.worxbend.aether.domain

import io.circe.Codec

case class AirGradientMeasures(
    wifi: Option[Int] = None,
    pm01: Option[Int] = None,
    pm02: Option[Int] = None,
    pm10: Option[Int] = None,
    pm003Count: Option[Int] = None,
    atmp: Option[Double] = None,
    rhum: Option[Int] = None,
    rco2: Option[Int] = None,
    tvoc: Option[Int] = None,
    tvocIndex: Option[Int] = None,
    noxIndex: Option[Int] = None,
    boot: Option[Long] = None,
    bootCount: Option[Int] = None,
    ledMode: Option[String] = None,
    firmwareVersion: Option[String] = None,
    serialno: Option[String] = None,
) derives Codec.AsObject

case class HealthResponse(
    status: String,
    upstreamUrl: String,
    hasMeasures: Boolean,
) derives Codec.AsObject
