package com.airgradient.proxy.upstream

import com.airgradient.proxy.domain.CachedSnapshot

trait AirGradientClient:
  def fetchCurrentMeasures(): Either[UpstreamError, CachedSnapshot]
  def fetchConfig():          Either[UpstreamError, Array[Byte]]
  def putConfig(payload: Array[Byte]): Either[UpstreamError, Array[Byte]]
