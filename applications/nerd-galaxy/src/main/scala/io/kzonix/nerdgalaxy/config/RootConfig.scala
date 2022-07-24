package io.kzonix.nerdgalaxy.config

case class RootConfig(
    appName: String,
    application: ApplicationConfig,
    database: DatabaseConfig) {}
