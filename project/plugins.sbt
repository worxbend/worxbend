logLevel := util.Level.Debug
ThisBuild / scalaVersion := "2.12.15"
ThisBuild / sbtVersion := "1.5.5"

dependsOn(
  RootProject(file("../plugins/common-build-settings"))
)



addSbtPlugin("org.scalameta"     % "sbt-scalafmt"        % "2.4.3")
addSbtPlugin("com.lightbend.sbt" % "sbt-java-formatter"  % "0.6.1")
addSbtPlugin("io.gatling"        % "gatling-sbt"         % "3.2.2")
addSbtPlugin("ch.epfl.scala"     % "sbt-bloop"           % "1.4.11")
addSbtPlugin("com.dwijnand"      % "sbt-dynver"          % "4.1.1")
addSbtPlugin("com.eed3si9n"      % "sbt-buildinfo"       % "0.10.0")
addSbtPlugin("com.typesafe.sbt"  % "sbt-native-packager" % "1.8.1")
addSbtPlugin("de.heikoseeberger" % "sbt-header"          % "5.6.0")
