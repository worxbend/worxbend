Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / sbtVersion := "1.6.1"
ThisBuild / useSuperShell := false
ThisBuild / autoStartServer := false

ThisBuild / update / evictionWarningOptions := EvictionWarningOptions.empty

addDependencyTreePlugin

addSbtPlugin("com.codecommit"    % "sbt-github-packages" % "0.5.3")
addSbtPlugin("com.dwijnand"      % "sbt-dynver"          % "4.1.1")
addSbtPlugin("com.typesafe.play" % "sbt-plugin"          % "2.8.13")

addSbtPlugin("ch.epfl.scala"     % "sbt-bloop"           % "1.4.11")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"        % "2.4.6")
addSbtPlugin("com.github.sbt"    % "sbt-unidoc"          % "0.5.0")
addSbtPlugin("com.typesafe.sbt"  % "sbt-git"             % "1.0.2")
addSbtPlugin("de.heikoseeberger" % "sbt-header"          % "5.6.0")
addSbtPlugin("com.typesafe.sbt"  % "sbt-native-packager" % "1.8.1")
