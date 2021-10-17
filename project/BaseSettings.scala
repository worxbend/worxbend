import sbt.Keys._
import sbt.Resolver
import sbt.Setting
import sbt.url

import java.text.SimpleDateFormat
import java.util.Calendar
import scala.language.postfixOps

object BaseSettings {

  val defaultSettings: Seq[Setting[_]] = Seq(
    versionScheme := Some("semver-spec"),
    startYear := Some(2020),
    version := "0.0.1.0-SNAPSHOT",
    scalaVersion := "3.1.0",
    resolvers ++= Seq(
      Resolver.mavenLocal,
      Resolver.mavenCentral,
      Resolver.sonatypeRepo("snapshots")
    )
  )

}
