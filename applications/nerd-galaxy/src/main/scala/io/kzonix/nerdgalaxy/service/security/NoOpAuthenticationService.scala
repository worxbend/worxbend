package io.kzonix.nerdgalaxy.service.security

import cats.data.EitherT
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import io.kzonix.nerdgalaxy.exceptions.AppRuntimeError
import io.kzonix.nerdgalaxy.SecureRouterComponents.UserContext
import io.kzonix.nerdgalaxy.SecureRouterComponents.EmptyUserContext

class NoOpAuthenticationService(name: String = "") extends AggregatedAuthenticationService with LazyLogging {

  override def serviceName: String = getServiceName(name)

  override def supportedType: AuthenticationService.AuthType = AuthenticationService.AuthType.All

  override def authenticate(authToken: String): EitherT[IO, AppRuntimeError, UserContext] =
    for {
      res <- EitherT.rightT[IO, AppRuntimeError](EmptyUserContext)
    } yield res

  override def canApply(authToken: String): EitherT[IO, AppRuntimeError, Boolean] = EitherT.rightT(true)

}
