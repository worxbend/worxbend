package io.kzonix.reqflect.routes

import zio.*
import zio.http.*
import zio.metrics.*
import zio.metrics.MetricKeyType.Counter

import java.time.temporal.ChronoUnit

object MetricsHttpMiddleware {

  private def requestCounter(method: String, handler: String): Metric[
    Counter,
    Any,
    MetricState.Counter,
  ] =
    Metric
      .counterInt("http_requests_total")
      .fromConst(0)
      .tagged(
        MetricLabel(
          "method",
          method,
        ),
        MetricLabel(
          "handler",
          handler,
        ),
      )

  private def requestHistogram(
      method: String,
      handler: String,
  ): Metric[
    MetricKeyType.Histogram,
    zio.Duration,
    MetricState.Histogram,
  ] =
    Metric
      .timer(
        "http_requests",
        ChronoUnit.MILLIS,
      )
      .fromConst(0.millis)
      .tagged(
        MetricLabel(
          "method",
          method,
        ),
        MetricLabel(
          "handler",
          handler,
        ),
      )

  // zio-http 3.x replaces RequestHandlerMiddleware with Middleware; wrap each
  // route handler via Routes#transform to log/time/count requests.
  val metricsMiddleware: Middleware[Any] =
    new Middleware[Any] {
      override def apply[Env1 <: Any, Err1](routes: Routes[Env1, Err1]): Routes[Env1, Err1] =
        routes.transform[Env1] { next =>
          Handler.fromFunctionZIO[Request] { request =>
            ZIO.scoped {
              for {
                _      <- ZIO.logInfo(
                            "Request handling: " +
                              s"method=${request.method} " +
                              s"headers=${request.method} " +
                              s"path=${request.url.encode} "
                          )
                result <- wrapWithMetrics(next, request)
                (duration, response) = result
                _      <- ZIO.logInfo(
                            "Request handled: " +
                              s"method=${request.method} " +
                              s"path=${request.url.encode} " +
                              s"status=${response.status.code} " +
                              s"duration=${duration.toMillis}ms"
                          )
              } yield response
            }
          }
        }
    }

  private def wrapWithMetrics[Env1, Err1](
      handler: Handler[
        Env1,
        Err1,
        Request,
        Response,
      ],
      request: Request,
  ) =
    handler.runZIO(request).timed
      @@ requestHistogram(
        request.method.toString,
        request.url.encode,
      ).trackDuration
      @@ requestCounter(
        request.method.toString,
        request.url.encode,
      )

}
