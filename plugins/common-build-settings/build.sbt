//import CompilerOptions._
Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val `common-build-settings` = project in file(".")

ThisBuild / sbtPlugin := true
ThisBuild / name := "CommonBuildSettings"
ThisBuild / organizationName := "N/A"
ThisBuild / organization := "io.kzonix"
ThisBuild / version ~= (
  _.replace(
    '+',
    '-'
  )
)
ThisBuild / dynver ~= (
  _.replace(
    '+',
    '-'
  )
)
ThisBuild / dynverSonatypeSnapshots := true
ThisBuild / dynverSeparator := "-"

ThisBuild / versionScheme := Some("early-semver")

ThisBuild / sbtVersion := "1.5.5"
ThisBuild / scalaVersion := "2.12.15"
ThisBuild / update / aggregate := false
ThisBuild / updateOptions := updateOptions.value
  .withCachedResolution(true)
  .withLatestSnapshots(false)
addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "1.4.11")
