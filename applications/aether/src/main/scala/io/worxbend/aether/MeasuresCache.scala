package io.worxbend.aether

import io.worxbend.aether.domain.AirGradientMeasures

import java.util.concurrent.atomic.AtomicReference

class MeasuresCache:
  private val ref = AtomicReference[Option[AirGradientMeasures]](None)

  def get: Option[AirGradientMeasures] = ref.get()

  def update(measures: AirGradientMeasures): Unit = ref.set(Some(measures))
