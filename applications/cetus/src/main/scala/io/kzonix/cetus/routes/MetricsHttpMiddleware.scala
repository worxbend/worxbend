package io.kzonix.cetus.routes

import zio.*
import zio.http.*
import zio.metrics.*
import zio.metrics.MetricKeyType.Counter
import zio.metrics.connectors.prometheus.*

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

  def metricsMiddleware: RequestHandlerMiddleware[
    Nothing,
    Any,
    Nothing,
    Any,
  ] =
    new RequestHandlerMiddleware.Simple[Any, Nothing] {
      override def apply[R1 <: Any, Err1 >: Nothing](
          handler: Handler[
            R1,
            Err1,
            Request,
            Response,
          ]
      )(implicit trace: Trace): Handler[
        R1,
        Err1,
        Request,
        Response,
      ] =
        Handler.fromFunctionZIO { request =>
          for {
            _                   <- ZIO.logInfo(">>>")
            result              <-
              handler.runZIO(request).timed
                @@ requestHistogram(
                  request.method.toString,
                  request.url.encode,
                ).trackDuration
                @@ requestCounter(
                  request.method.toString,
                  request.url.encode,
                )
            (duration, response) = result
            _                   <- ZIO.logInfo(
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
