package io.kzonix

import sbt._
import sbt.Keys._
import sbt.Resolver
import sbt.Setting
import sbt.url

object ProjectDefaults {

  val ProjectSettings: Seq[Def.Setting[_]] = Seq(
    ThisBuild / homepage := Some(url("https://kzonix.app")),
    ThisBuild / licenses += ("MIT", url("https://www.gnu.org/licenses/gpl-2.0.html")),
  )

  val OrganizationSettings: Seq[Def.Setting[_]] = Seq(
    ThisBuild / organization     := "io.kzonix",
    ThisBuild / organizationName := "Kzonix",
  )

}
