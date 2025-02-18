package io.kzonix.reqflect.services.models

import java.time.ZonedDateTime

case class RequestInfo(
    remoteAddress: String,
    path:          String,
    host:          String,
    method:        String,
    headers:       Map[String, String],
)
