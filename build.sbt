import BaseSettings._
import Dependencies._
import ProjectUtils._
import com.typesafe.sbt.SbtNativePackager.Docker
import sbt.Test

/* -- START: Global Settings -- */
Global / onChangedBuildSource := ReloadOnSourceChanges
Global / homepage := Some(url("http://recursive-escalator.io"))
Global / startYear := Some(2018)

/* -- END:   Global Settings  -- */

lazy val commonSettings = {
  commonDependencies ++ testDependencies ++ defaultSettings
}

/* Root project
 * ---------------------------------------------------------------------------------------------------------------------
 * ---------------------------------------------------------------------------------------------------------------------
 * ---------------------------------------------------------------------------------------------------------------------
 * */

lazy val kzonix = (project in file("."))
  .settings(defaultSettings: _*)
  .settings(
    name := "kzonix"
  )
  .aggregate(
    `twitee-service`,
    `index-service`,
    `redprime-service`
  )

/*
 * ---------------------------------------------------------------------------------------------------------------------
 * ---------------------------------------------------------------------------------------------------------------------
 * ---------------------------------------------------------------------------------------------------------------------
 * */

/* --- Play Framework related components --- */

lazy val `play-utile`: Project = (project in file(ProjectPaths.Components.Play.lib(Seq("play-utile"))))
  .enablePlugins(PlayService)
  .settings(commonSettings: _*)
  .settings(
    name := ProjectNames.lib("play-utile"),
    libraryDependencies ++= Seq(
      guice,
      scalaGuice,
      logback
    ),
    Test / testOptions := Seq(
      Tests.Argument(
        TestFrameworks.JUnit,
        "-a",
        "-v"
      )
    )
  )
  .dependsOn(`sird-provider`)
  .aggregate(`sird-provider`)

lazy val `sird-provider-api` = (project in file(ProjectPaths.Components.Play.api(Seq("sird-provider"))))
  .enablePlugins(PlayService)
  .settings(defaultSettings: _*)
  .settings(
    name := ProjectNames.api("sird-provider"),
    libraryDependencies ++= Seq(
      guice,
      scalaGuice,
      logback
    ),
    Test / testOptions := Seq(
      Tests.Argument(
        TestFrameworks.JUnit,
        "-a",
        "-v"
      )
    )
  )

lazy val `sird-provider` = (project in file(ProjectPaths.Components.Play.lib(Seq("sird-provider"))))
  .enablePlugins(PlayService)
  .settings(commonSettings: _*)
  .settings(
    version := Utils.Versions.version(),
    name := ProjectNames.lib("sird-provider"),
    libraryDependencies ++= Seq(
      guice,
      scalaGuice,
      logback
    ),
    Test / testOptions := Seq(
      Tests.Argument(
        TestFrameworks.JUnit,
        "-a",
        "-v"
      )
    )
  )
  .dependsOn(`sird-provider-api`)
  .aggregate(`sird-provider-api`)

lazy val `play-underpressure-api` = (project in file(
  ProjectPaths.Components.Play.api(
    Seq(
      "play",
      "play-underpressure"
    )
  )
))
  .enablePlugins(PlayService)
  .settings(commonSettings: _*)
  .settings(
    name := ProjectNames.api("play-underpressure"),
    libraryDependencies ++= Seq(
      guice,
      scalaGuice,
      logback
    ),
    Test / testOptions := Seq(
      Tests.Argument(
        TestFrameworks.JUnit,
        "-a",
        "-v"
      )
    )
  )

lazy val `play-underpressure` = (project in file(
  ProjectPaths.Components.Play.lib(
    Seq(
      "play",
      "play-underpressure"
    )
  )
))
  .enablePlugins(PlayService)
  .settings(commonSettings: _*)
  .settings(
    name := ProjectNames.lib("play-underpressure"),
    libraryDependencies ++= Seq(
      guice,
      scalaGuice,
      logback
    ),
    Test / testOptions := Seq(
      Tests.Argument(
        TestFrameworks.JUnit,
        "-a",
        "-v"
      )
    )
  )
  .dependsOn(`play-underpressure-api`)
  .aggregate(`play-underpressure-api`)

/* ------- Common Library ------- */

lazy val `cogwheel` = (project in file(ProjectPaths.Components.Common.lib(Seq("cogwheel"))))
  .settings(commonSettings: _*)
  .settings(
    name := ProjectNames.service("cogwheel"),
    Compile / run / mainClass := Some("io.kzonix.cogwheel.Main"),
    libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-java-sdk-ssm" % "1.12.68"
    ),
    Test / testOptions := Seq(
      Tests.Argument(
        TestFrameworks.ScalaTest,
        "-oD"
      )
    )
  )

/* -- Sandbox Applications -- */
lazy val `scala3-sandbox` = (project in file(ProjectPaths.Applications.Sandbox.app(Seq("scala3-sandbox"))))
  .settings(scala3: _*)
  .settings(
    name := ProjectNames.app("scala3-sandbox")
  )

lazy val `akka-quickstart-service` =
  (project in file(ProjectPaths.Applications.Sandbox.service(Seq("akka-quickstart"))))
    .enablePlugins(
      AutomateHeaderPlugin,
      BuildInfoPlugin,
      DockerPlugin,
      JavaAppPackaging
    )
    .settings(commonDependencies: _*)
    .settings(
      scalaVersion := "2.13.6",
      name := ProjectNames.service("quickstart"),
      libraryDependencies ++= Seq(
        guice,
        scalaGuice,
        logbackLogging,
        alpakkaKafka,
        kafkaClients
      )
        ++ akka
        ++ akkaTest
        ++ circe
        ++ micrometerPrometheus
        ++ pureConfig,
      Test / testOptions := Seq(
        Tests.Argument(
          TestFrameworks.JUnit,
          "-a",
          "-v"
        )
      )
    )

lazy val `akka-cluster-bootstrap-service` =
  (project in file(ProjectPaths.Applications.Sandbox.service(Seq("akka-cluster-bootstrap"))))
    .enablePlugins(
      AutomateHeaderPlugin,
      BuildInfoPlugin,
      DockerPlugin,
      JavaAppPackaging
    )
    .settings(defaultSettings: _*)
    .settings(commonDependencies: _*)
    .settings(
      scalaVersion := "2.13.6",
      name := ProjectNames.service("akka-cluster-bootstrap"),
      libraryDependencies ++= Seq(
        guice,
        scalaGuice,
        logbackLogging,
        alpakkaKafka,
        kafkaClients
      )
        ++ akka
        ++ akkaTest
        ++ circe
        ++ micrometerPrometheus
        ++ pureConfig,
      Test / testOptions := Seq(
        Tests.Argument(
          TestFrameworks.JUnit,
          "-a",
          "-v"
        )
      )
    )

/* ------- Applications ------ */

lazy val `redprime-service` = (project in file(ProjectPaths.Applications.Root.service(Seq("redprime"))))
  .enablePlugins(PlayService)
  .settings(commonSettings: _*)
  .settings(
    name := ProjectNames.service("redprime"),
    libraryDependencies ++= Seq(
      filters,
      caffeine,
      guice,
      scalaGuice,
      logback,
      ws
    ),
    Test / testOptions := Seq(
      Tests.Argument(
        TestFrameworks.JUnit,
        "-a",
        "-v"
      )
    )
  )
  .dependsOn(
    `sird-provider`,
    `play-utile`
  )
  .aggregate(
    `sird-provider`,
    `play-utile`
  )

lazy val `index-service` = (project in file(ProjectPaths.Applications.Root.service(Seq("index"))))
  .enablePlugins(PlayService)
  .settings(commonSettings: _*)
  .settings(
    name := ProjectNames.service("index"),
    libraryDependencies ++= Seq(
      filters,
      caffeine,
      guice,
      scalaGuice,
      logback,
      "com.azure" % "azure-storage-blob" % "12.13.0"
    ),
    Test / testOptions := Seq(
      Tests.Argument(
        TestFrameworks.JUnit,
        "-a",
        "-v"
      )
    )
  )
  .dependsOn(
    `sird-provider`,
    `play-utile`
  )
  .aggregate(
    `sird-provider`,
    `play-utile`
  )

lazy val `twitee-service` = (project in file(ProjectPaths.Applications.Root.service(Seq("twitee"))))
  .enablePlugins(PlayService)
  .settings(commonSettings: _*)
  .settings(
    name := ProjectNames.service("redprime"),
    libraryDependencies ++= Seq(
      filters,
      caffeine,
      guice,
      scalaGuice,
      logback,
      ws
    ),
    Test / testOptions := Seq(
      Tests.Argument(
        TestFrameworks.JUnit,
        "-a",
        "-v"
      )
    )
  )
  .dependsOn(
    `sird-provider`,
    `play-utile`
  )
  .aggregate(
    `sird-provider`,
    `play-utile`
  )

lazy val `hresvelgr` = (project in file(ProjectPaths.Applications.Root.app(Seq("hresvelgr"))))
  .enablePlugins(
    AutomateHeaderPlugin,
    BuildInfoPlugin,
    DockerPlugin,
    JavaAppPackaging,
    AshScriptPlugin
  )
  .settings(defaultSettings: _*)
  .settings(commonDependencies: _*)
  .settings(
    scalaVersion := "2.13.6",
    name := ProjectNames.app("hresvelgr"),
    libraryDependencies ++= Seq(
      logbackLogging
    )
      ++ akka
      ++ akkaTest
      ++ circe
      ++ micrometerPrometheus
      ++ pureConfig,
    Test / testOptions := Seq(
      Tests.Argument(
        TestFrameworks.JUnit,
        "-a",
        "-v"
      )
    ),
    Docker / dockerBaseImage := "openjdk"
  )

// Scalafmt
addCommandAlias(
  "fmt",
  "; compile:scalafmt; test:scalafmt; scalafmtSbt"
)
addCommandAlias(
  "fmtCheck",
  "; compile:scalafmtCheck; test:scalafmtCheck; scalafmtSbtCheck"
)
