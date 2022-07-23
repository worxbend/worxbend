import BaseSettings._
import Dependencies._
import com.typesafe.sbt.SbtNativePackager.Docker
import sbt.Test

/* -- BEGIN: Global Settings -- */
Global / onChangedBuildSource := ReloadOnSourceChanges
Global / startYear := Some(2018)
/* -- END:   Global Settings  -- */

/* -- BEGIN: inThisBuild */
ThisBuild / scalaVersion := "3.1.0"

/* -- END:   inThisBuild */

lazy val commonSettings = defaultSettings

/* -- BEGIN: Root project -- */
lazy val `kzonix-mono` = (project in file("."))
  .enablePlugins(
    CommonBuildSettings,
    ProjectBuilder
  )
  .settings(defaultSettings: _*)
  .settings(
    name := "kzonix-mono"
  )
  .aggregate(
    `common-build-settings`,
    `project-builder`,
    `project-dependencies`,
  )

/* -- END:   Root project -- */

/* -- BEGIN: Plugins -- */
lazy val `common-build-settings` = ProjectRef(
  file("plugins/common-build-settings"),
  "common-build-settings"
)

lazy val `project-builder` = ProjectRef(
  file("plugins/project-builder"),
  "project-builder"
)


lazy val `project-dependencies` = ProjectRef(
  file("plugins/project-dependencies"),
  "project-dependencies"
)

/* -- END:   Plugins -- */
