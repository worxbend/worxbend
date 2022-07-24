package io.kzonix.nerdgalaxy

import akka.actor.typed.ActorSystem
import akka.actor.typed.SpawnProtocol
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import akka.http.scaladsl.Http
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.PhaseActorSystemTerminate
import akka.actor.CoordinatedShutdown.PhaseBeforeServiceUnbind

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class AkkaHttpServerApplication(
    config: Config,
    system: ActorSystem[SpawnProtocol.Command],
    router: ApplicationRouter)
    extends ServerApplication
       with LazyLogging {

  import cats.effect.IO

  implicit val as: ActorSystem[SpawnProtocol.Command] = system
  implicit val executor: ExecutionContext             = system.executionContext

  def start(): IO[Unit] =
    IO {

      logger.info("Starting Akka HTTP server instance...")
      logger.debug(config.root().render())

      val _: Future[Http.ServerBinding] =
        for {
          serverBinding <- startHttpServer
          _             <- Future(logger.info(s"The server has been successfully started: ${ serverBinding.toString }."))
        } yield serverBinding

      CoordinatedShutdown(as).addTask(
        PhaseBeforeServiceUnbind,
        "clean-up-server-resources",
      ) { () =>
        Future {
          import akka.Done
          logger.info("Before server termination")
          Done.done()
        }
      }

      CoordinatedShutdown(as).addTask(
        PhaseActorSystemTerminate,
        "clean-up-server-resources",
      ) { () =>
        Future {
          import akka.Done
          logger.info("Actor system termination")
          Done.done()
        }
      }
    } *>
      IO.fromFuture(IO(as.whenTerminated)) *>
      IO.unit

  private def startHttpServer: Future[Http.ServerBinding] =
    Http()
      .newServerAt(
        config.getString("application.server.http.interface"),
        config.getInt("application.server.http.port"),
      )
      .bindFlow(router.routes)

}

object AkkaHttpServerApplication {}
