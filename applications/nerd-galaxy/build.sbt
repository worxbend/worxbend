import sbtassembly.AssemblyPlugin.defaultShellScript

ThisBuild / version := "0.1.0-SNAPSHOT"

val AkkaVersion     = "2.6.19"
val AkkaHttpVersion = "10.2.9"
val TapirVersion    = "1.0.0-RC1"
val CirceVersion    = "0.14.2"
val Http4sVersion   = "1.0.0-M32"
val MacwireVersion  = "2.5.7"

ThisBuild / scalaVersion               := "2.13.8"
ThisBuild / assemblyPrependShellScript := Some(defaultShellScript)
ThisBuild / scalacOptions ++= ScalacOptions.Common
ThisBuild / scalafixScalaBinaryVersion := CrossVersion.binaryScalaVersion(scalaVersion.value)
Global / scalafixScalaBinaryVersion    := CrossVersion.binaryScalaVersion(scalaVersion.value)

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
      "commons-io"                   % "commons-io"               % "2.11.0",
      "org.apache.commons"           % "commons-math"             % "2.2",
      "org.scala-lang.modules"      %% "scala-java8-compat"       % "1.0.2",
      "org.slf4j"                    % "slf4j-api"                % "1.7.9",
      "org.reactivemongo"           %% "reactivemongo-scalafix"   % "1.1.0-RC4",
      "org.mongodb.scala"           %% "mongo-scala-driver"       % "4.6.0",
      "com.github.pureconfig"       %% "pureconfig"               % "0.17.1",
      "com.typesafe"                 % "config"                   % "1.4.2",
      "com.typesafe.scala-logging"  %% "scala-logging"            % "3.9.5",
      "ch.qos.logback"               % "logback-classic"          % "1.2.11",
      "io.kamon"                    %% "kamon-core"               % "2.5.3",
      "io.kamon"                    %% "kamon-system-metrics"     % "2.5.3",
      "io.kamon"                    %% "kamon-testkit"            % "2.5.3"      % Test,
      "io.circe"                    %% "circe-core"               % CirceVersion,
      "io.circe"                    %% "circe-generic"            % CirceVersion,
      "io.circe"                    %% "circe-parser"             % CirceVersion,
      "io.circe"                    %% "circe-testing"            % CirceVersion % Test,
      "io.circe"                    %% "circe-generic-extras"     % CirceVersion,
      "io.circe"                    %% "circe-jawn"               % CirceVersion,
      "org.typelevel"               %% "cats-core"                % "2.7.0",
      "org.typelevel"               %% "cats-effect"              % "3.3.12",
      "org.typelevel"               %% "cats-mtl"                 % "1.2.0",
      "org.typelevel"               %% "kittens"                  % "3.0.0-M4",
      "org.typelevel"               %% "cats-collections-core"    % "0.9.0",
      "com.typesafe.akka"           %% "akka-slf4j"               % AkkaVersion,
      "com.typesafe.akka"           %% "akka-actor-typed"         % AkkaVersion,
      "com.typesafe.akka"           %% "akka-actor"               % AkkaVersion,
      "com.typesafe.akka"           %% "akka-discovery"           % AkkaVersion,
      "com.typesafe.akka"           %% "akka-coordination"        % AkkaVersion,
      "com.typesafe.akka"           %% "akka-cluster-typed"       % AkkaVersion,
      "com.typesafe.akka"           %% "akka-stream"              % AkkaVersion,
      "com.typesafe.akka"           %% "akka-actor-testkit-typed" % AkkaVersion  % Test,
      "com.typesafe.akka"           %% "akka-testkit"             % AkkaVersion  % Test,
      "com.typesafe.akka"           %% "akka-http"                % AkkaHttpVersion,
      "com.typesafe.akka"           %% "akka-http-testkit"        % AkkaHttpVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-core"               % TapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle"  % TapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-redoc-bundle"       % TapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"         % TapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-akka-http-server"   % TapirVersion,
      "com.softwaremill.macwire"    %% "macros"                   % MacwireVersion,
      "com.softwaremill.macwire"    %% "util"                     % MacwireVersion,
      "com.softwaremill.macwire"    %% "proxy"                    % MacwireVersion,
    ),
  )
