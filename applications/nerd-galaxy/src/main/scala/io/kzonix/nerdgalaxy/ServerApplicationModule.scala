package io.kzonix.nerdgalaxy

import com.typesafe.config.Config
import akka.actor.typed.ActorSystem
import cats.effect.unsafe.IORuntime
import com.softwaremill.macwire.Module
import io.kzonix.nerdgalaxy.routes.GamesEndpoints
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter

import scala.concurrent.ExecutionContext
import com.softwaremill.macwire.wire
import com.softwaremill.macwire.wireSet
import io.kzonix.nerdgalaxy.ServerApplicationModule.createActorSystem
import io.kzonix.nerdgalaxy.service.security.AuthenticationService
import io.kzonix.nerdgalaxy.service.security.JwtAuthenticationService
/** The main components of the server application */
@Module
class ServerApplicationModule(runtime: IORuntime, configModule: ServerApplicationConfigModule) {

  import akka.actor.typed.SpawnProtocol
  import configModule._

  implicit lazy val system: ActorSystem[SpawnProtocol.Command] = createActorSystem(
    rootConfig.appName,
    rawConfig,
  )
  implicit lazy val ec: ExecutionContext                       = system.executionContext
  implicit lazy val appRuntime: IORuntime                      = runtime

  lazy val akkaHttpServerInterpreter: AkkaHttpServerInterpreter = AkkaHttpServerInterpreter()(system.executionContext)
  // authentication providers
  lazy val jwtAuthenticationService: AuthenticationService      = wire[JwtAuthenticationService]
  lazy val authenticationServices: Set[AuthenticationService]   = wireSet[AuthenticationService]
  // router components
  lazy val routerComponents: RouterComponents                   = wire[RouterComponents]
  lazy val secureRouterComponents: SecureRouterComponents       = wire[SecureRouterComponents]
  // routes wiring
  lazy val gamesServerEndpoints: GamesEndpoints                 = wire[GamesEndpoints]
  // preparing a set of routes
  lazy val routes: Set[ServerEndpoints]                         = wireSet[ServerEndpoints]
  lazy val applicationRouter: ApplicationRouter                 = wire[AkkaHttpApplicationRouter]
  lazy val serverApplication: ServerApplication                 = wire[AkkaHttpServerApplication]

}

object ServerApplicationModule {

  import akka.actor.typed.SpawnProtocol

  private def createActorSystem(
      appName: String,
      config: Config,
    ): ActorSystem[SpawnProtocol.Command] =
    ActorSystem.create(
      GuardianActor(),
      appName,
      config,
    )

}
