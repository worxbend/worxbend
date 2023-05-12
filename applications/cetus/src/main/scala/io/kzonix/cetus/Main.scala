package io.kzonix.cetus

import zio.{Console, ExitCode, IO, RIO, Scope, Task, UIO, URIO, ZIO, ZIOAppArgs}
import zio.http.Body
import zio.http.Client
import zio.http.Response
import zio.http.ZClient
import izumi.distage.model.Locator
import izumi.distage.model.definition.Lifecycle
import izumi.distage.modules.support.ZIOSupportModule.*
import izumi.functional.quasi.QuasiIO
import izumi.fundamentals.platform.functional.Identity

import java.time.LocalDateTime
import distage.Activation
import distage.DefaultModule
import distage.Injector
import distage.Module
import distage.ModuleDef
import distage.Roots
import distage.TagK
import io.kzonix.cetus.DistageExample.Environment

trait Random {
  def nextInteger: UIO[Int]
}

final class ScalaRandom extends Random {
  override def nextInteger: UIO[Int] = ZIO.succeed(scala.util.Random.nextInt())
}

trait Logger {
  def log(name: String): Task[Unit]
}

final class ConsoleLogger extends Logger {
  override def log(line: String): Task[Unit] = {
    val timeStamp = LocalDateTime.now()
    ZIO.attempt(println(s"$timeStamp: $line"))
  }
}

final class RandomApp(
    random: Random,
    logger: Logger,
    //client: Client,
  ) {

//  private def makeReq() = {
//    val url = "https://httpbin.org/anything"
//
//    client
//      .get(
//        url
//      )
//      .onError(e => ZIO.succeed(Response.text(e.prettyPrint)))
//  }
  def run: ZIO[Any, Throwable, Unit] =
    for {
//      res <- makeReq()
//      resStr <- res.body.asString
      random <- random.nextInteger
//      _ <- ZIO.logInfo(resStr)
      _      <- logger.log(s"random number generated: $random")
      _      <- ZIO.logInfo(s"random number generated: $random")
    } yield ()
}

object DistageExample extends zio.ZIOAppDefault {

  def RandomAppModule: ModuleDef =
    new ModuleDef {

      make[Random].from[ScalaRandom]
      make[Logger].from[ConsoleLogger]
//      make[Client].fromHas(Client.default)
      make[RandomApp] // `.from` is not required for concrete classes

    }

  val injector = Injector[ZIO[Environment & (zio.ZIOAppArgs & zio.Scope), Any, _]]()

  val resource = injector.produce(
    bindings = RandomAppModule,
    activation = Activation.empty,
    roots = Roots.target[RandomApp],
  )

  val effect: Identity[ZIO[Any, Throwable, Unit]] = resource.use { objects =>
    objects.get[RandomApp].run
  }

  def run: zio.ZIO[Environment & (zio.ZIOAppArgs & zio.Scope), Any, ExitCode] = effect.exitCode

}
