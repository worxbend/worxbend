
import Versions._

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val `project-dependencies` = project in file(".")

ThisBuild / sbtVersion       := SbtVersion
/*
 * sbt plugins must be compiled with Scala 2.12.x that sbt itself is compiled in.
 * By NOT specifying scalaVersion, sbt will default to the Scala version suited for a plugin.
 * - By default sbt plugin is compiled with whichever the sbt version you are using.
 * Because sbt does NOT keep forward compatibility, that would typically require all of your plugin users to upgrade
 * to the latest too.
 * */

ThisBuild / sbtPlugin        := true
ThisBuild / name             := "ProjectDependencies"
ThisBuild / organizationName := "N/A"
ThisBuild / organization     := "io.kzonix"

ThisBuild / version       := "0.0.1.0-SNAPSHOT"
ThisBuild / versionScheme := Some("early-semver")

ThisBuild / update / aggregate := true
ThisBuild / updateOptions      := updateOptions
  .value
  .withCachedResolution(false)
  .withLatestSnapshots(false)

