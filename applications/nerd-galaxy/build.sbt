import sbtassembly.AssemblyPlugin.defaultShellScript

ThisBuild / version := "0.1.0-SNAPSHOT"

val AkkaVersion     = "2.7.0"
val AkkaHttpVersion = "10.4.0"
val TapirVersion    = "1.2.8"
val CirceVersion    = "0.14.4"
val Http4sVersion   = "1.0.0-M32"
val MacwireVersion  = "2.5.8"

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

lazy val `nerd-galaxy` = (project in file("."))
  .enablePlugins(
    UniversalPlugin,
    JavaAppPackaging,
  )
  .settings(commonSettings)
  .settings(
    name                             := "nerd-galaxy",
    Compile / mainClass              := Some("io.kzonix.nerdgalaxy.Main"),
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
      "commons-io"                   % "commons-io"               % "20030203.000550",
      "org.apache.commons"           % "commons-math"             % "2.2",
      "org.scala-lang.modules"      %% "scala-java8-compat"       % "1.0.2",
      "org.slf4j"                    % "slf4j-api"                % "2.0.5",
      "com.typesafe"                 % "config"                   % "1.4.2",
      "com.typesafe.scala-logging"  %% "scala-logging"            % "3.9.5",
      "ch.qos.logback"               % "logback-classic"          % "1.4.5",
      "io.kamon"                    %% "kamon-core"               % "2.6.0",
      "io.kamon"                    %% "kamon-system-metrics"     % "2.6.0",
      "io.kamon"                    %% "kamon-testkit"            % "2.6.0"      % Test,
      "io.circe"                    %% "circe-core"               % CirceVersion,
      "io.circe"                    %% "circe-generic"            % CirceVersion,
      "io.circe"                    %% "circe-parser"             % CirceVersion,
      "io.circe"                    %% "circe-testing"            % CirceVersion % Test,
      "io.circe"                    %% "circe-jawn"               % CirceVersion,
      "org.typelevel"               %% "cats-core"                % "2.9.0",
      "org.typelevel"               %% "cats-effect"              % "3.4.7",
      "org.typelevel"               %% "cats-mtl"                 % "1.3.0",
      "org.typelevel"               %% "kittens"                  % "3.0.0-M4",
      "org.typelevel"               %% "cats-collections-core"    % "0.9.5",

      "com.softwaremill.sttp.tapir" %% "tapir-core"               % TapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle"  % TapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-redoc-bundle"       % TapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"         % TapirVersion,
      "com.softwaremill.macwire"    %% "macros"                   % MacwireVersion,
      "com.softwaremill.macwire"    %% "util"                     % MacwireVersion,
      "com.softwaremill.macwire"    %% "proxy"                    % MacwireVersion,
    ),
  )
