import sbtassembly.AssemblyPlugin.defaultShellScript

ThisBuild / version := "0.1.0-SNAPSHOT"

val CirceVersion = "0.14.5"

ThisBuild / scalaVersion               := "3.6.4"
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

lazy val `cetus` = (project in file("."))
  .enablePlugins(
    UniversalPlugin,
    JavaAppPackaging,
    GraalVMNativeImagePlugin,
  )
  .settings(commonSettings)
  .settings(
    name                             := "cetus",
    Compile / mainClass              := Some("io.kzonix.cetus.CetusApp"),
    assembly / mainClass             := (Compile / mainClass).value,
    assembly / assemblyJarName       := s"${name.value}-${version.value}.jar",
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
      case PathList("META-INF", "maven", _*)     => MergeStrategy.last
      case PathList("META-INF", "resources", _*) => MergeStrategy.last
      case PathList("META-INF", _*)              => MergeStrategy.discard
      case _                                     => MergeStrategy.first
    },
    libraryDependencies ++= Seq(
      "dev.zio"        %% "zio"                    % "2.0.13",
      "dev.zio"        %% "zio-streams"            % "2.0.13",
      "dev.zio"        %% "zio-config"             % "4.0.0-RC14",
      "dev.zio"        %% "zio-config-magnolia"    % "4.0.0-RC14",
      "dev.zio"        %% "zio-config-typesafe"    % "4.0.0-RC14",
      "dev.zio"        %% "zio-config-refined"     % "4.0.0-RC14",
      "dev.zio"        %% "zio-http"               % "3.0.0-RC1",
      "dev.zio"        %% "zio-json"               % "0.5.0",
      "dev.zio"        %% "zio-logging"            % "2.1.12",
      "dev.zio"        %% "zio-metrics-connectors" % "2.0.8",
      "io.7mind.izumi" %% "distage-core"           % "1.1.0-M21",
    ),
  )
