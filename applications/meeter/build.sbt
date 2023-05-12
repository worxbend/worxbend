import sbtassembly.AssemblyPlugin.defaultShellScript

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion               := "3.2.0"
ThisBuild / assemblyPrependShellScript := Some(defaultShellScript)
ThisBuild / scalacOptions ++= ScalacOptions.Common

lazy val commonSettings =
  commonScalacOptions ++ Seq(
    update / evictionWarningOptions := EvictionWarningOptions.empty
  )

lazy val commonScalacOptions = Seq(
  Compile / console / scalacOptions := ScalacOptions.Common,
  Test / console / scalacOptions    :=
    (Compile / console / scalacOptions).value,
)

lazy val meeter = (project in file("."))
  .enablePlugins(
    UniversalPlugin,
    JavaAppPackaging,
    AssemblyPlugin,
  )
  .settings(commonSettings)
  .settings(
    name                             := "meeter",
    Compile / mainClass              := Some("com.worxbend.meeter.MeeterApp"),
    assembly / mainClass             := (Compile / mainClass).value,
    assembly / assemblyJarName       := s"${ name.value }-${ version.value }.jar",
    assembly / assemblyCacheOutput   := false,
    assembly / aggregate             := false,
    assembly / compile               := (Compile / compile).value,
    assembly / test                  := Seq(),
    assembly / assemblyMergeStrategy := {
      case "logback.xml"                         => MergeStrategy.discard
      case "logback-test.xml"                    => MergeStrategy.discard
      case x if x.endsWith("application.conf")   => MergeStrategy.discard
      case x if x.endsWith("reference.conf")     => MergeStrategy.concat
      case x if x.endsWith(".example")           => MergeStrategy.discard
      case PathList("META-INF", "services", _*)  => MergeStrategy.concat
      case PathList("META-INF", "maven", _*)     => MergeStrategy.last
      case PathList("META-INF", "resources", _*) => MergeStrategy.last
      case PathList("META-INF", _*)              => MergeStrategy.discard
      case _                                     => MergeStrategy.first
    },
    libraryDependencies ++= Seq(
      "dev.zio"                     %% "zio"                    % "2.0.13",
      "dev.zio"                     %% "zio-streams"            % "2.0.13",
      "dev.zio"                     %% "zio-config"             % "4.0.0-RC14",
      "dev.zio"                     %% "zio-config-magnolia"    % "4.0.0-RC14",
      "dev.zio"                     %% "zio-config-typesafe"    % "4.0.0-RC14",
      "dev.zio"                     %% "zio-config-refined"     % "4.0.0-RC14",
      "dev.zio"                     %% "zio-http"               % "3.0.0-RC1",
      "dev.zio"                     %% "zio-json"               % "0.5.0",
      "dev.zio"                     %% "zio-logging"            % "2.1.12",
      "dev.zio"                     %% "zio-metrics-connectors" % "2.0.8",
      "dev.zio"                     %% "zio-interop-cats"       % "23.0.03",
      "dev.zio"                     %% "zio-prelude"            % "1.0.0-RC19",
      "com.softwaremill.sttp.tapir" %% "tapir-zio"              % "1.3.0",
      "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server"  % "1.3.0",
      "com.softwaremill.sttp.tapir" %% "tapir-json-zio"         % "1.3.0",
    ),
  )
