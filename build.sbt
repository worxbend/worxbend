import BaseSettings._
import Dependencies._
import ProjectUtils._
import com.typesafe.sbt.SbtNativePackager.Docker
import sbt.Test

/* -- START: Global Settings -- */
Global / onChangedBuildSource := ReloadOnSourceChanges
Global / homepage := Some(url("https://recursive-escalator.io"))
Global / startYear := Some(2018)

/* -- END:   Global Settings  -- */

lazy val commonSettings =
  defaultSettings

/* Root project
 * ---------------------------------------------------------------------------------------------------------------------
 * ---------------------------------------------------------------------------------------------------------------------
 * ---------------------------------------------------------------------------------------------------------------------
 * */

lazy val `kzonix-mono` = (project in file("."))
  .settings(defaultSettings: _*)
  .settings(
    name := "kzonix-mono"
  )

/*
 * ---------------------------------------------------------------------------------------------------------------------
 * ---------------------------------------------------------------------------------------------------------------------
 * ---------------------------------------------------------------------------------------------------------------------
 * */
