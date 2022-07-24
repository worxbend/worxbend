package io.kzonix.nerdgalaxy

import cats.effect.IO

trait ServerApplication {
  def start(): IO[Unit]
}
