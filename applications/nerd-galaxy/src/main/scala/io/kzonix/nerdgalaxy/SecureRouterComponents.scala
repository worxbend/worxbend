package io.kzonix.nerdgalaxy

import cats.effect.unsafe.IORuntime
import com.typesafe.scalalogging.LazyLogging
import io.kzonix.nerdgalaxy.model.http.ApiErrorResponse
import io.kzonix.nerdgalaxy.SecureRouterComponents.EmptyUserContext
import sttp.tapir.EndpointInput.AuthType
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.server.PartialServerEndpoint
import sttp.model.headers.WWWAuthenticateChallenge
import scala.concurrent.Future
import io.kzonix.nerdgalaxy.SecureRouterComponents.UserContext
import io.kzonix.nerdgalaxy.config.AuthConfig
import io.kzonix.nerdgalaxy.service.security.ApiKeyAuthenticationService
import io.kzonix.nerdgalaxy.service.security.AuthenticationService
import io.kzonix.nerdgalaxy.service.security.BasicAuthenticationService
import io.kzonix.nerdgalaxy.service.security.DefaultAggregatedAuthenticationService
import io.kzonix.nerdgalaxy.service.security.AuthenticationService.AuthType.All
import io.kzonix.nerdgalaxy.service.security.JwtAuthenticationService

// TODO: add service based approach to deal with 'serverSecurityLogic'
class SecureRouterComponents(
    authConfig: AuthConfig,
    controllerComponents: RouterComponents,
    authServices: Set[AuthenticationService],
  )(implicit
    runtime: IORuntime)
    extends LazyLogging {

  import io.kzonix.nerdgalaxy.SecureRouterComponents.secureInTypes

  private lazy val authServicesMap: Map[AuthenticationService.AuthType, Set[AuthenticationService]] = authServices
    .groupBy(el => el.supportedType)
  private lazy val secureInputs                                                                     = secureInTypes()

  val basicAuthSecureEndpoint
      : PartialServerEndpoint[String, UserContext, Unit, (StatusCode, ApiErrorResponse), Unit, Any, Future] =
    constructSecureEndpoint(BasicAuthenticationService.Basic)

  val apiKeyAuthSecureEndpoint
      : PartialServerEndpoint[String, UserContext, Unit, (StatusCode, ApiErrorResponse), Unit, Any, Future] =
    constructSecureEndpoint(ApiKeyAuthenticationService.ApiKey)

  val jwtAuthSecureEndpoint
      : PartialServerEndpoint[String, UserContext, Unit, (StatusCode, ApiErrorResponse), Unit, Any, Future] =
    constructSecureEndpoint(JwtAuthenticationService.Jwt)

  private def constructSecureEndpoint(authType: AuthenticationService.AuthType) =
    controllerComponents
      .baseEndpoint
      .securityIn(secureInputs(authType))
      .in(authConfig.pathPrefix)
      .serverSecurityLogic[UserContext, Future] { authToken =>
        Future(
          fetchAuthService(authType)
            .authenticate(authToken)
            .value
            .unsafeRunSync())(runtime.compute)
        Future.successful(Right.apply[(StatusCode, ApiErrorResponse), UserContext](EmptyUserContext))
      }

  private def fetchAuthService(authType: AuthenticationService.AuthType) =
    new DefaultAggregatedAuthenticationService(authServicesMap
                                                 .get(authType)
                                                 .orElse(authServicesMap.get(All))
                                                 .getOrElse(Set.empty),
                                               name = authType.getClass.getSimpleName)

}

object SecureRouterComponents {

  trait UserContext
  case object EmptyUserContext extends UserContext
  case class UserIdentity(
      id: String,
      name: String,
      email: String)
      extends UserContext

  def secureInTypes(
    ): Map[AuthenticationService.AuthType, EndpointInput.Auth[String, _ >: AuthType.Http with AuthType.ApiKey <: AuthType with Serializable]] =
    Set(
      (BasicAuthenticationService.Basic,
       AuthStrategy.custom[AuthType.Http](
         headerName = "Authorization",
         authScheme = "Basic",
         scheme => AuthType.Http(scheme),
         challenge = WWWAuthenticateChallenge.apply("Basic"),
       )),
      (ApiKeyAuthenticationService.ApiKey,
       AuthStrategy.custom(
         headerName = "X-API-Key",
         authScheme = "Bearer",
         _ => AuthType.ApiKey(),
         challenge = WWWAuthenticateChallenge.apply("API Key"),
       )),
      (JwtAuthenticationService.Jwt,
       AuthStrategy.custom(
         headerName = "Authorization",
         authScheme = "Bearer",
         scheme => AuthType.Http(scheme),
         challenge = WWWAuthenticateChallenge.apply("Bearer"),
       )),
    ).toMap

  object AuthStrategy {

    import sttp.tapir.EndpointInput.AuthType

    def custom[T <: AuthType](
        headerName: String,
        authScheme: String,
        authType: String => T,
        challenge: WWWAuthenticateChallenge,
      ): EndpointInput.Auth[String, T] = {
      val codec                                              = implicitly[Codec[List[String], String, CodecFormat.TextPlain]]
      def filterHeaders(headers: List[String]): List[String] =
        headers.filter(_.toLowerCase.startsWith(authScheme.toLowerCase))

      def stringPrefixWithSpace: Mapping[List[String], List[String]] =
        Mapping.stringPrefixCaseInsensitiveForList(authScheme + " ")

      val authCodec = Codec
        .id[List[String], CodecFormat.TextPlain](
          codec.format,
          Schema.binary,
        )
        .map((e: List[String]) => filterHeaders(e))(identity)
        .map(stringPrefixWithSpace)
        .mapDecode(codec.decode)(codec.encode)
        .schema(codec.schema)

      EndpointInput.Auth(
        header[String](headerName)(authCodec),
        challenge,
        authType(authScheme),
        EndpointInput.AuthInfo.Empty,
      )
    }

  }

}
