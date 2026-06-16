package com.airgradient.proxy.domain

import sttp.tapir.Schema.annotations.description

// Documentation-only model — upstream JSON is passed through as raw bytes and is never
// deserialized into this type.  Field set covers known AirGradient sensor variants; all
// fields are optional because not every device model includes every measurement.
final case class WifiInfo(
  @description("WiFi RSSI signal strength in dBm")
  rssi: Int,
)

@description("AirGradient local API sensor measurement snapshot")
final case class AirGradientMeasures(
  wifi:       Option[WifiInfo],
  @description("PM1.0 concentration µg/m³") pm01:       Option[Double],
  @description("PM2.5 concentration µg/m³") pm02:       Option[Double],
  @description("PM10 concentration µg/m³")  pm10:       Option[Double],
  @description("Particle count >0.3 µm per 0.1 L") pm003Count: Option[Int],
  @description("Air temperature °C")        atmp:       Option[Double],
  @description("Relative humidity %")       rhum:       Option[Double],
  @description("CO₂ ppm")                   rco2:       Option[Int],
  @description("TVOC index (1–500)")        tvoc:       Option[Int],
  @description("NOx index (1–500)")         nox:        Option[Int],
  @description("Ozone ppb")                 o3:         Option[Double],
)
