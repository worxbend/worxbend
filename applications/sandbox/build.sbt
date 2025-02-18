import sbtassembly.AssemblyPlugin.defaultShellScript

ThisBuild / scalaVersion               := "3.4.2"
ThisBuild / assemblyPrependShellScript := Some(defaultShellScript)
ThisBuild / scalacOptions ++= ScalacOptions.Common

lazy val commonSettings: Seq[Setting[_]] =
  commonScalacOptions ++ Seq(
    update / evictionWarningOptions := EvictionWarningOptions.empty
  )

lazy val commonScalacOptions = Seq(
  Compile / console / scalacOptions := ScalacOptions.Common,
  Test / console / scalacOptions    :=
    (Compile / console / scalacOptions).value,
)

lazy val `sandbox` = (project in file("."))
  .settings(commonSettings)
  .settings(
    name                             := "sandbox",
    Compile / mainClass              := Some("com.worxbend.sandbox.SandboxApp"),
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
    run / fork                       := true,
    libraryDependencies ++= Seq(
      "com.softwaremill.magnolia1_3" %% "magnolia" % "1.3.3"
    ),
  )
