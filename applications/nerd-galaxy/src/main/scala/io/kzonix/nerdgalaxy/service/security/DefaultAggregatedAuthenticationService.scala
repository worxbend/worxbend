package io.kzonix.nerdgalaxy.service.security

import cats.data.EitherT
import cats.effect.IO
import cats.syntax.monad._
import com.typesafe.scalalogging.LazyLogging
import io.kzonix.nerdgalaxy.exceptions.AppError
import io.kzonix.nerdgalaxy.exceptions.AppRuntimeError
import io.kzonix.nerdgalaxy.exceptions.errors.auth.NoSupportedAuth
import io.kzonix.nerdgalaxy.exceptions.errors.auth.NotImplementedYet
import io.kzonix.nerdgalaxy.SecureRouterComponents.UserContext
import io.kzonix.nerdgalaxy.service.security.DefaultAggregatedAuthenticationService.authenticateWith
import io.kzonix.nerdgalaxy.service.security.AuthenticationService.AuthType.All

class DefaultAggregatedAuthenticationService(authenticationServices: Set[AuthenticationService], name: String = "")
    extends AggregatedAuthenticationService
       with LazyLogging {

  override def serviceName: String = getServiceName(name)

  override def authenticate(authToken: String): EitherT[IO, AppRuntimeError, UserContext] =
    if (authenticationServices.isEmpty)
      for {
        _   <- EitherT.rightT[IO, AppRuntimeError](logger.warn("No instances are provided..."))
        res <- EitherT.leftT[IO, UserContext](AppError(NotImplementedYet))
      } yield res
    else
      authenticateWith(
        authToken,
        authenticationServices,
      )

  override def canApply(authToken: String): EitherT[IO, AppRuntimeError, Boolean] = EitherT.rightT(true)

  override def supportedType: AuthenticationService.AuthType = All

}

object DefaultAggregatedAuthenticationService extends LazyLogging {

  case class Execution(
      token: String,
      services: Set[AuthenticationService],
      exceptionTrace: List[AppRuntimeError],
      result: Option[UserContext])
  private def performAction(exec: Execution): IO[Execution] = {
    val services = exec.services
    if (services.nonEmpty) {
      val current                                              = services.head
      val triedAuth: EitherT[IO, AppRuntimeError, UserContext] =
        for {
          canApply <- current.canApply(exec.token)
          res      <-
            if (canApply) {
              logger.info(
                s"The instance '${ current.getClass.getSimpleName } - ${ current.serviceName }' can be applied to handle authentication")
              current.authenticate(exec.token)
            }
            else {
              logger.info(
                s"The instance '${ current.getClass.getSimpleName } - ${ current.serviceName }' can not be applied to handle authentication")
              EitherT.leftT[IO, UserContext](AppError(NoSupportedAuth))
            }
        } yield res

      IO.cede *> triedAuth.value.map {
        case Left(ex)                        =>
          exec.copy(
            services = services.tail,
            exceptionTrace = ex :: exec.exceptionTrace,
          )
        case Right(userContext: UserContext) =>
          exec.copy(
            services = services.tail,
            result = Some(userContext),
          )
      }
    }
    else
      IO.pure(exec)
  }

  private def authenticateWith(
      authToken: String,
      authenticationServices: Set[AuthenticationService],
    ): EitherT[IO, AppRuntimeError, UserContext] = {
    val execResult: IO[Execution] =
      Execution(
        authToken,
        authenticationServices,
        List.empty,
        None,
      ).iterateUntilM(performAction)(_.result.nonEmpty)

    EitherT(execResult.map { executionResult =>
      executionResult.result.toRight {
        val err =
          if (executionResult.exceptionTrace.nonEmpty) {
            val err = executionResult.exceptionTrace.head
            logger.error(
              "Failed to authenticate user request:",
              err,
            )
            err
          }
          else {
            logger.warn(
              s"There are no eligible services to perform authentication of of user-request: [$executionResult].")
            AppError(NoSupportedAuth)
          }
        err
      }
    })
  }

}
