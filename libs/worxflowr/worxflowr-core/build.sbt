ThisBuild / version := "0.1.0-SNAPSHOT"

val CirceVersion = "0.14.5"

ThisBuild / scalaVersion := "3.6.4"

lazy val `worxflowr-core` = (project in file("."))
  .settings(
    name := "worxflowr-core",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"                    % "2.0.13",
      "dev.zio" %% "zio-streams"            % "2.0.13",
      "dev.zio" %% "zio-config"             % "4.0.0-RC14",
      "dev.zio" %% "zio-config-magnolia"    % "4.0.0-RC14",
      "dev.zio" %% "zio-config-typesafe"    % "4.0.0-RC14",
      "dev.zio" %% "zio-config-refined"     % "4.0.0-RC14",
      "dev.zio" %% "zio-http"               % "0.0.5",
      "dev.zio" %% "zio-json"               % "0.5.0",
      "dev.zio" %% "zio-logging"            % "2.1.12",
      "dev.zio" %% "zio-metrics-connectors" % "2.0.8",
      "dev.zio" %% "zio-cache"              % "0.2.3",

      /* --- */
      "io.github.arainko" %% "ducktape" % "0.1.4",

      /* --- */
      "io.lettuce" % "lettuce-core" % "6.2.3.RELEASE",
    ),
  )
