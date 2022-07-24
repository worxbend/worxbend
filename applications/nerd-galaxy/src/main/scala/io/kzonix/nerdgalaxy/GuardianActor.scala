package io.kzonix.nerdgalaxy

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior
import akka.actor.typed.SpawnProtocol

object GuardianActor {
  def apply(): Behavior[SpawnProtocol.Command] =
    Behaviors.setup { ctx =>
      ctx.log.info(s"Setup guardian actor '${ this.getClass.getSimpleName }''")
      SpawnProtocol()
    }
}
