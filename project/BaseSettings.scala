import scala.language.postfixOps

import sbt.Keys._
import sbt.Resolver
import sbt.Setting

object BaseSettings {

  val defaultSettings: Seq[Setting[_]] = Seq(
    versionScheme := Some("semver-spec"),
    startYear     := Some(2020),
    version       := "0.0.1.0-SNAPSHOT",
    scalaVersion  := "3.2.0",
    resolvers ++= Seq(
      Resolver.mavenLocal,
      Resolver.mavenCentral,
    ) ++ Resolver.sonatypeOssRepos("snapshots"),
  )
}
