import Versions.Bloop
import Versions.Scala
import Versions.Sbt

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val `{{cookiecutter.project_slug}}` = project in file(".")

ThisBuild / sbtVersion := Sbt
/*
 * sbt plugins must be compiled with Scala 2.12.x that sbt itself is compiled in.
 * By NOT specifying scalaVersion, sbt will default to the Scala version suited for a plugin.
 * - By default sbt plugin is compiled with whichever the sbt version you are using.
 * Because sbt does NOT keep forward compatibility, that would typically require all of your plugin users to upgrade
 * to the latest too.
 * */

ThisBuild / name := "{{cookiecutter.project_name}}"
ThisBuild / organizationName := "{{cookiecutter.organization_name}}"
ThisBuild / organization := "{{cookiecutter.organization_package}}"

ThisBuild / version := "{{cookiecutter.project_version}}"
ThisBuild / versionScheme := Some("early-semver")

ThisBuild / update / aggregate := true
ThisBuild / updateOptions := updateOptions.value
  .withCachedResolution(false)
  .withLatestSnapshots(false)

addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % Bloop)
