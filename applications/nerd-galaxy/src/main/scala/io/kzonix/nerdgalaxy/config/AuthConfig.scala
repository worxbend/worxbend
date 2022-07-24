package io.kzonix.nerdgalaxy.config

import io.kzonix.nerdgalaxy.config.AuthConfig.AuthProvider

case class AuthConfig(
    pathPrefix: String,
    providers: Set[AuthProvider])

object AuthConfig {

  sealed trait AuthProvider

  case class Jwt(privateKey: String)    extends AuthProvider
  case class Basic(creds: List[String]) extends AuthProvider
  case class ApiKey(keys: List[String]) extends AuthProvider

}
