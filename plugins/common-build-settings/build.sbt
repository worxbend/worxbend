Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val `common-build-settings` = project in file(".")

ThisBuild / sbtVersion := "1.6.1"
/*
 * sbt plugins must be compiled with Scala 2.12.x that sbt itself is compiled in.
 * By NOT specifying scalaVersion, sbt will default to the Scala version suited for a plugin.
 * By default sbt plugin is compiled with whichever the sbt version you are using.
 * Because sbt does NOT keep forward compatibility, that would typically require all of your plugin users to upgrade
 * to the latest too.
 * */

ThisBuild / sbtPlugin := true
ThisBuild / name := "CommonBuildSettings"
ThisBuild / organizationName := "N/A"
ThisBuild / organization := "io.kzonix"

ThisBuild / version := "0.0.1.2-SNAPSHOT"
ThisBuild / versionScheme := Some("early-semver")

ThisBuild / update / aggregate := true
ThisBuild / updateOptions := updateOptions.value
  .withCachedResolution(false)
  .withLatestSnapshots(false)

enablePlugins(SbtPlugin)

// All the plugins that are provided as transitive plugin-dependencies for all user builds that refers to this plugin.
addSbtPlugin("ch.epfl.scala"     % "sbt-bloop"           % "1.4.11")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"        % "2.4.6")
addSbtPlugin("com.github.sbt"    % "sbt-unidoc"          % "0.5.0")
addSbtPlugin("com.typesafe.sbt"  % "sbt-git"             % "1.0.2")
addSbtPlugin("com.timushev.sbt"  % "sbt-rewarn"          % "0.1.3")
addSbtPlugin("com.timushev.sbt"  % "sbt-updates"         % "0.6.1")
addSbtPlugin("io.spray"          % "sbt-revolver"        % "0.9.1")
addSbtPlugin("de.heikoseeberger" % "sbt-header"          % "5.6.0")
addSbtPlugin("com.typesafe.sbt"  % "sbt-native-packager" % "1.8.1")
addDependencyTreePlugin
