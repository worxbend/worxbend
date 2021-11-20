Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / scalaVersion := "2.12.15"
ThisBuild / sbtVersion := "1.5.5"
ThisBuild / useSuperShell := false
ThisBuild / autoStartServer := false
ThisBuild / update / evictionWarningOptions := EvictionWarningOptions.empty
update / evictionWarningOptions := EvictionWarningOptions.empty

addDependencyTreePlugin

addSbtPlugin("com.codecommit" % "sbt-github-packages" % "0.5.3")
addSbtPlugin("ch.epfl.scala"  % "sbt-bloop"           % "1.4.11")

addSbtPlugin("org.scalameta"  % "sbt-scalafmt" % "2.4.3")
addSbtPlugin("ch.epfl.scala"  % "sbt-scalafix" % "0.9.31")
addSbtPlugin("com.github.sbt" % "sbt-unidoc"   % "0.5.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.2")

addSbtPlugin("io.spray" % "sbt-revolver"    % "0.9.1")
addSbtPlugin("io.spray" % "sbt-boilerplate" % "0.6.1")

addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.6.0")
addSbtPlugin("com.dwijnand"      % "sbt-dynver" % "4.1.1")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.4")
