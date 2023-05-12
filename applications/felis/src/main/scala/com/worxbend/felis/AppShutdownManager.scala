package com.worxbend.felis

import cats.effect.IO

trait AppShutdownManager {

  def add(name: String, steps: Seq[IO[Unit]]): IO[Unit]
  def replace(name: String, steps: Seq[IO[Unit]]): IO[Unit]
  def remove(name: String): IO[Seq[IO[Unit]]]
  def clean(): IO[Map[String, Seq[IO[Unit]]]]

}
