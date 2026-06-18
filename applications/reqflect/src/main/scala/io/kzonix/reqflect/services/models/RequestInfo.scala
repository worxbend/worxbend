package io.kzonix.reqflect.services.models

case class RequestInfo(
    remoteAddress: String,
    path:          String,
    host:          String,
    method:        String,
    headers:       Map[String, String],
)
