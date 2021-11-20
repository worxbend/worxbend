package io.kzonix

import sbt._
import sbt.Keys._
import sbt.Resolver
import sbt.Setting
import sbt.url

object BaseSettings {

  val ProjectSettings: Seq[Def.Setting[_]] = Seq(
    description := "Project Description",
    startYear := Some(2020),
    homepage := Some(url("https://kzonix.app")),
    licenses += ("MIT", url("https://www.gnu.org/licenses/gpl-2.0.html")),
    developers := List(
      Developer(
        "limpid-kzonix",
        "Alexander Balyshyn",
        "balyszyn@gmail.com",
        url("https://me.kzonix.app")
      )
    )
  )

  val OrganizationSettings: Seq[Def.Setting[_]] = Seq(
    organization := "io.kzonix",
    organizationName := "N/A"
  )

}
