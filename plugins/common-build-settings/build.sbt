import Versions.Bloop
import Versions.Scala
import Versions.Sbt

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val `common-build-settings` = project in file(".")

ThisBuild / sbtVersion := Sbt
ThisBuild / scalaVersion := Scala

ThisBuild / sbtPlugin := true
ThisBuild / name := "CommonBuildSettings"
ThisBuild / organizationName := "N/A"
ThisBuild / organization := "io.kzonix"

ThisBuild / version := "0.0.1.0-SNAPSHOT"
ThisBuild / versionScheme := Some("early-semver")

ThisBuild / update / aggregate := true
ThisBuild / updateOptions := updateOptions.value
  .withCachedResolution(false)
  .withLatestSnapshots(false)

addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % Bloop)
