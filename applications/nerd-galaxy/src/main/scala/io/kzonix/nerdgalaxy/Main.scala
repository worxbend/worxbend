package io.kzonix.nerdgalaxy

import cats.effect.IOApp
import com.typesafe.scalalogging.LazyLogging
import cats.effect.IO

object Main extends IOApp.Simple with LazyLogging {

  logger.info("Application startup...")
  private lazy val serverAppModule = new ServerApplicationModule(runtime, new ServerApplicationConfigModule)
  def run: IO[Unit]                = serverAppModule.serverApplication.start()

}
