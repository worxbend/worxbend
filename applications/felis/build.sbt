import sbtassembly.AssemblyPlugin.defaultShellScript

ThisBuild / version := "0.1.0-SNAPSHOT"

val CirceVersion = "0.14.4"

ThisBuild / scalaVersion               := "3.2.0"
ThisBuild / assemblyPrependShellScript := Some(defaultShellScript)
ThisBuild / scalacOptions ++= ScalacOptions.Common

lazy val commonSettings =
  commonScalacOptions ++ Seq(
    update / evictionWarningOptions := EvictionWarningOptions.empty
  )

lazy val commonScalacOptions = Seq(
  Compile / console / scalacOptions --= Seq(
    "-Wunused:_",
    "-Xfatal-warnings",
  ),
  Test / console / scalacOptions :=
    (Compile / console / scalacOptions).value,
)

lazy val `felis` = (project in file("."))
  .enablePlugins(
    UniversalPlugin,
    JavaAppPackaging,
  )
  .settings(commonSettings)
  .settings(
    name                             := "felis",
    Compile / mainClass              := Some("io.kzonix.felis.Main"),
    assembly / mainClass             := (Compile / mainClass).value,
    assembly / assemblyJarName       := s"${ name.value }-${ version.value }.jar",
    assembly / assemblyCacheOutput   := false,
    assembly / aggregate             := false,
    assembly / compile               := (Compile / compile).value,
    assembly / test                  := Seq(),
    assembly / assemblyMergeStrategy := {
      case "logback.xml"                         => MergeStrategy.discard
      case "logback-test.xml"                    => MergeStrategy.discard
      case x if x.endsWith(".conf")              => MergeStrategy.concat
      case x if x.endsWith(".example")           => MergeStrategy.concat
      case PathList("META-INF", "services", _*)  => MergeStrategy.concat
      case PathList("META-INF", "maven", _*)     => MergeStrategy.singleOrError
      case PathList("META-INF", "resources", _*) => MergeStrategy.last
      case PathList("META-INF", _*)              => MergeStrategy.discard
      case _                                     => MergeStrategy.first
    },
    libraryDependencies ++= Seq(
      "commons-io"                  % "commons-io"         % "20030203.000550",
      "org.apache.commons"          % "commons-math"       % "2.2",
      "org.scala-lang.modules"     %% "scala-java8-compat" % "1.0.2",
      "org.slf4j"                   % "slf4j-api"          % "2.0.5",
      "com.typesafe"                % "config"             % "1.4.2",
      "com.typesafe.scala-logging" %% "scala-logging"      % "3.9.5",
      "ch.qos.logback"              % "logback-classic"    % "1.4.5",
      "io.circe"                   %% "circe-core"         % CirceVersion,
      "io.circe"                   %% "circe-generic"      % CirceVersion,
      "io.circe"                   %% "circe-parser"       % CirceVersion,
      "io.circe"                   %% "circe-testing"      % CirceVersion % Test,
      "io.circe"                   %% "circe-jawn"         % CirceVersion,
    ),
  )
