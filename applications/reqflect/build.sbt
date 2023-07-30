import sbtassembly.AssemblyPlugin.defaultShellScript

ThisBuild / version := "0.1.0-SNAPSHOT"

val CirceVersion = "0.14.5"

ThisBuild / scalaVersion               := "3.3.0"
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

lazy val `reqflect` = (project in file("."))
  .enablePlugins(
    UniversalPlugin,
    JavaAppPackaging,
    GraalVMNativeImagePlugin,
  )
  .settings(commonSettings)
  .settings(
    name                             := "reqflect",
    Compile / mainClass              := Some("io.kzonix.reqflect.ReqflectApp"),
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
      case PathList("META-INF", "maven", _*)     => MergeStrategy.last
      case PathList("META-INF", "resources", _*) => MergeStrategy.last
      case PathList("META-INF", _*)              => MergeStrategy.discard
      case _                                     => MergeStrategy.first
    },
    run / fork                       := true,
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"                    % "2.0.12",
      "dev.zio" %% "zio-streams"            % "2.0.12",
      "dev.zio" %% "zio-config"             % "4.0.0-RC14",
      "dev.zio" %% "zio-config-magnolia"    % "4.0.0-RC14",
      "dev.zio" %% "zio-config-typesafe"    % "4.0.0-RC14",
      "dev.zio" %% "zio-config-refined"     % "4.0.0-RC14",
      "dev.zio" %% "zio-http"               % "0.0.5",
      "dev.zio" %% "zio-json"               % "0.5.0",
      "dev.zio" %% "zio-schema"             % "0.4.12",
      "dev.zio" %% "zio-schema-json"        % "0.4.12",
      "dev.zio" %% "zio-logging"            % "2.1.12",
      "dev.zio" %% "zio-metrics-connectors" % "2.0.7",
      "dev.zio" %% "zio-cache"              % "0.2.3",

      /* --- */
      "io.github.arainko" %% "ducktape" % "0.1.4",
    ),
    graalVMNativeImageGraalVersion   := Some("22.3.0"),
    graalVMNativeImageOptions ++= Seq(
      "-H:IncludeResources=logback.xml",
      "-H:+ReportUnsupportedElementsAtRuntime",
      "-H:+JNI",
      "--add-opens=java.base/java.nio=ALL-UNNAMED",
      "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
      "--add-opens=java.base/jdk.internal.ref=ALL-UNNAMED",
      "--initialize-at-run-time=io.netty",
      "-H:ConfigurationResourceRoots=/opt/graalvm/",
    ),
  )
