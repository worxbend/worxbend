package io.kzonix.nerdgalaxy

import com.softwaremill.macwire.Module
import io.kzonix.nerdgalaxy.config.RootConfig
import com.typesafe.config.ConfigFactory
import io.kzonix.nerdgalaxy.ServerApplicationConfigModule.decodeConfig
import io.kzonix.nerdgalaxy.config.ApplicationConfig
import io.kzonix.nerdgalaxy.config.DatabaseConfig
import pureconfig.generic.auto._
import pureconfig.ConfigSource
import com.typesafe.config.Config

/** The static config instances */
@Module
class ServerApplicationConfigModule() {

  import io.kzonix.nerdgalaxy.config.AuthConfig
  import io.kzonix.nerdgalaxy.config.HttpConfig
  import io.kzonix.nerdgalaxy.config.ServerConfig

  lazy val rawConfig: Config                    = ConfigFactory.load()
  lazy val rootConfig: RootConfig               = decodeConfig(rawConfig)
  lazy val applicationConfig: ApplicationConfig = rootConfig.application
  lazy val serverAppConfig: ServerConfig        = applicationConfig.server
  lazy val httpConfig: HttpConfig               = serverAppConfig.http
  lazy val authenticationConfig: AuthConfig     = httpConfig.auth
  lazy val databaseConfig: DatabaseConfig       = rootConfig.database

}

object ServerApplicationConfigModule {
  private def decodeConfig(config: Config) =
    ConfigSource
      .fromConfig(config)
      .withFallback(ConfigSource.default)
      .loadOrThrow[RootConfig]
}
