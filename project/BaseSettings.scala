import scala.language.postfixOps

import sbt.Keys.*
import sbt.*

object BaseSettings {

  val defaultSettings: Seq[Setting[_]] = Seq(
    versionScheme := Some("semver-spec"),
    startYear     := Some(2020),
    version       := "0.0.1.0-SNAPSHOT",
    scalaVersion  := "3.6.4",
    resolvers ++= Seq(
      Resolver.mavenLocal,
      Resolver.mavenCentral,
    ) ++ Resolver.sonatypeOssRepos("snapshots"),
  )
}
