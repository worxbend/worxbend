package io.kzonix.nerdgalaxy.service.security

import cats.data.EitherT
import cats.effect.IO
import io.kzonix.nerdgalaxy.exceptions.AppRuntimeError
import io.kzonix.nerdgalaxy.SecureRouterComponents.UserContext
import io.kzonix.nerdgalaxy.service.security.AuthenticationService.AuthType

trait AuthenticationService {

  override def toString: String = serviceName
  def serviceName: String       = this.getClass.getSimpleName
  def supportedType: AuthType
  def authenticate(authToken: String): EitherT[IO, AppRuntimeError, UserContext]
  def canApply(authToken: String): EitherT[IO, AppRuntimeError, Boolean]

  def getServiceName(name: String): String =
    if (name.nonEmpty)
      name
    else
      this.serviceName

}

object AuthenticationService {

  trait AuthType

  object AuthType {
    case object All extends AuthType
  }

}
