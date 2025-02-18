import sbtassembly.AssemblyPlugin.defaultShellScript

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion               := "3.6.3"
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

lazy val `inaya` = (project in file("."))
  .enablePlugins(
    UniversalPlugin,
    JavaAppPackaging,
    GraalVMNativeImagePlugin,
  )
  .settings(commonSettings)
  .settings(
    name                             := "inaya",
    Compile / mainClass              := Some("io.worxbend.inaya.InayaApp"),
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
    libraryDependencies ++= Seq(),
  )
