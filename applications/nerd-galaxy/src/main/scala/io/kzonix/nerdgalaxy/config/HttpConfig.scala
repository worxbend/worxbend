package io.kzonix.nerdgalaxy.config

case class HttpConfig(
    interface: String,
    port: Int,
    basePath: String = "/",
    apiVersion: Int = 1,
    auth: AuthConfig) {}
