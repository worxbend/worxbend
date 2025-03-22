logLevel                 := util.Level.Debug
ThisBuild / scalaVersion := "2.12.20"
ThisBuild / sbtVersion   := "1.10.11"

dependsOn(
  ProjectRef(
    file("../plugins/common-build-settings"),
    "common-build-settings",
  )
)

dependsOn(
  ProjectRef(
    file("../plugins/project-builder"),
    "project-builder",
  )
)

addSbtPlugin("ch.epfl.scala"     % "sbt-scalafix"        % "0.14.0")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"        % "2.5.4")
addSbtPlugin("io.gatling"        % "gatling-sbt"         % "3.2.2")
addSbtPlugin("com.dwijnand"      % "sbt-dynver"          % "4.1.1")
addSbtPlugin("com.eed3si9n"      % "sbt-buildinfo"       % "0.10.0")
addSbtPlugin("com.github.sbt"    % "sbt-native-packager" % "1.9.15")
addSbtPlugin("de.heikoseeberger" % "sbt-header"          % "5.6.0")
