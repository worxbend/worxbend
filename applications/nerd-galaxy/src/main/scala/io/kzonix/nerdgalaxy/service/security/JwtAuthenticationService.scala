package io.kzonix.nerdgalaxy.service.security

import cats.data.EitherT
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import io.kzonix.nerdgalaxy.exceptions.AppRuntimeError
import io.kzonix.nerdgalaxy.SecureRouterComponents.UserContext
import io.kzonix.nerdgalaxy.service.security.AuthenticationService.AuthType
import io.kzonix.nerdgalaxy.service.security.JwtAuthenticationService.Jwt

class JwtAuthenticationService extends AuthenticationService with LazyLogging {

  import io.kzonix.nerdgalaxy.SecureRouterComponents.EmptyUserContext

  override def authenticate(authToken: String): EitherT[IO, AppRuntimeError, UserContext] =
    for {
      res <- EitherT.rightT[IO, AppRuntimeError](EmptyUserContext)
      _   <- EitherT.rightT[IO, AppRuntimeError] {
               logger.info(authToken)
             }
    } yield res

  override def canApply(authToken: String): EitherT[IO, AppRuntimeError, Boolean] = EitherT.rightT(true)

  override def supportedType: AuthType = Jwt

}

object JwtAuthenticationService {
  case object Jwt extends AuthType
}
