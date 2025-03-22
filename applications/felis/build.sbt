import sbtassembly.AssemblyPlugin.defaultShellScript

ThisBuild / version := "0.1.0-SNAPSHOT"

val CirceVersion = "0.14.4"

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

lazy val `felis` = (project in file("."))
  .enablePlugins(
    UniversalPlugin,
    JavaAppPackaging,
  )
  .settings(commonSettings)
  .settings(
    name                             := "felis",
    Compile / mainClass              := Some("io.worxbend.felis.Main"),
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
      case PathList("META-INF", "maven", _*)     => MergeStrategy.singleOrError
      case PathList("META-INF", "resources", _*) => MergeStrategy.last
      case PathList("META-INF", _*)              => MergeStrategy.discard
      case _                                     => MergeStrategy.first
    },
    libraryDependencies ++= Seq(
      "org.typelevel"  %% "cats-effect"  % "3.4.10",
      "org.typelevel"  %% "cats-core"    % "2.9.0",
      "io.7mind.izumi" %% "distage-core" % "1.1.0-M21",
    ),
  )
