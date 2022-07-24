package io.kzonix.nerdgalaxy.routes

import com.typesafe.scalalogging.LazyLogging
import io.kzonix.nerdgalaxy.model.http.ApiSuccessResponse
import sttp.capabilities.WebSockets
import sttp.capabilities.akka.AkkaStreams
import sttp.tapir._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import io.kzonix.nerdgalaxy.model.http.ApiErrorResponse
import sttp.model.StatusCode
import io.kzonix.nerdgalaxy.SecureRouterComponents
import io.kzonix.nerdgalaxy.ServerEndpoints
import io.kzonix.nerdgalaxy.model.games.Game
import sttp.tapir.server.ServerEndpoint.Full

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

class GamesEndpoints(
    secureRouterComponents: SecureRouterComponents
  )(implicit
    ec: ExecutionContext)
    extends ServerEndpoints
       with LazyLogging {

  /* The relative routePath of the current route */
  private val routePath: String = "games"

  val createGame
      : Full[String, SecureRouterComponents.UserContext, Game, (StatusCode, ApiErrorResponse), ApiSuccessResponse[Game], Any, Future] =
    secureRouterComponents
      .jwtAuthSecureEndpoint
      .in(routePath)
      .post
      .in(jsonBody[Game])
      .out(jsonBody[ApiSuccessResponse[Game]])
      .serverLogic(user =>
        game =>
          Future {
            logger.info(user.toString)
            Right.apply[(StatusCode, ApiErrorResponse), ApiSuccessResponse[Game]](ApiSuccessResponse[Game](game))
          })

  val updateGame
      : Full[String, SecureRouterComponents.UserContext, Game, (StatusCode, ApiErrorResponse), ApiSuccessResponse[Game], Any, Future] =
    secureRouterComponents
      .jwtAuthSecureEndpoint
      .in(routePath)
      .put
      .in(jsonBody[Game])
      .out(jsonBody[ApiSuccessResponse[Game]])
      .serverLogic(user =>
        game =>
          Future {
            logger.info(user.toString)
            Right.apply[(StatusCode, ApiErrorResponse), ApiSuccessResponse[Game]](ApiSuccessResponse[Game](game))
          })

  val partialUpdateGame
      : Full[String, SecureRouterComponents.UserContext, Game, (StatusCode, ApiErrorResponse), ApiSuccessResponse[Game], Any, Future] =
    secureRouterComponents
      .jwtAuthSecureEndpoint
      .in(routePath)
      .patch
      .in(jsonBody[Game])
      .out(jsonBody[ApiSuccessResponse[Game]])
      .serverLogic(user =>
        game =>
          Future {
            logger.info(user.toString)
            Right.apply[(StatusCode, ApiErrorResponse), ApiSuccessResponse[Game]](ApiSuccessResponse[Game](game))
          })

  val deleteGame
      : Full[String, SecureRouterComponents.UserContext, String, (StatusCode, ApiErrorResponse), ApiSuccessResponse[Game], Any, Future] =
    secureRouterComponents
      .jwtAuthSecureEndpoint
      .in(routePath)
      .delete
      .in(path[String])
      .out(jsonBody[ApiSuccessResponse[Game]])
      .serverLogic(user =>
        game =>
          Future {
            logger.info(user.toString)
            Right.apply[(StatusCode, ApiErrorResponse), ApiSuccessResponse[Game]](
              ApiSuccessResponse[Game](
                Game(
                  game,
                  "",
                )
              )
            )
          })

  val getGame
      : Full[String, SecureRouterComponents.UserContext, String, (StatusCode, ApiErrorResponse), ApiSuccessResponse[Game], Any, Future] =
    secureRouterComponents
      .jwtAuthSecureEndpoint
      .in(routePath)
      .get
      .in(path[String])
      .out(jsonBody[ApiSuccessResponse[Game]])
      .serverLogic(user =>
        game =>
          Future {
            logger.info(user.toString)
            Right.apply[(StatusCode, ApiErrorResponse), ApiSuccessResponse[Game]](
              ApiSuccessResponse[Game](
                Game(
                  game,
                  "",
                )
              )
            )
          })

  val listGames
      : Full[String, SecureRouterComponents.UserContext, Unit, (StatusCode, ApiErrorResponse), ApiSuccessResponse[Game], Any, Future] =
    secureRouterComponents
      .apiKeyAuthSecureEndpoint
      .in(routePath)
      .get
      .out(jsonBody[ApiSuccessResponse[Game]])
      .serverLogic(user =>
        _ =>
          Future {
            logger.info(user.toString)
            Right.apply[(StatusCode, ApiErrorResponse), ApiSuccessResponse[Game]](
              ApiSuccessResponse[Game](
                Game(
                  "",
                  "",
                )
              )
            )
          })

  override def endpoints: List[ServerEndpoint[AkkaStreams with WebSockets, Future]] =
    List(
      createGame,
      getGame,
      listGames,
      updateGame,
      partialUpdateGame,
      deleteGame,
    )

}
