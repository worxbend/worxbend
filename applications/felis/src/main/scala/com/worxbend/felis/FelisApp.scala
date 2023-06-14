package com.worxbend.felis

import izumi.distage.model.Locator
import izumi.distage.model.definition.Lifecycle

import scala.concurrent.duration.DurationInt

import cats.effect.IO
import cats.effect.IOApp
import distage.Injector
import distage.ModuleDef
import distage.Roots

class FelisApp(
    appDependencies: FelisDependencies
  ):

  def start(): IO[Unit] =
    IO {
      println(appDependencies.appConfig.name)
      ()
    }.delayBy(1.seconds).foreverM
  def stop(): IO[Unit]  =
    IO {
      println("stopping")
      ()
    }

object FelisApp extends IOApp.Simple:

  private def rootModule: ModuleDef =
    new ModuleDef {
      make[FelisApp]
    }

  val injector: Lifecycle[IO, Locator] = Injector[IO]()
    .produce(
      rootModule ++ FelisModule.felisModule,
      Roots.target[FelisApp],
    )

  var run: IO[Unit] =
    injector
      .use(locator => IO(locator.get[FelisApp]))
      .bracket(_.start())(_.stop())
