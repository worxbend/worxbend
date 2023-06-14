Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / sbtVersion      := "1.8.2"
ThisBuild / useSuperShell   := false
ThisBuild / autoStartServer := false

ThisBuild / update / evictionWarningOptions := EvictionWarningOptions.empty

addDependencyTreePlugin

addSbtPlugin("com.github.sbt"   % "sbt-git"      % "2.0.0")
addSbtPlugin("com.timushev.sbt" % "sbt-rewarn"   % "0.1.3")
addSbtPlugin("com.timushev.sbt" % "sbt-updates"  % "0.6.4")
addSbtPlugin("io.spray"         % "sbt-revolver" % "0.9.1")
addSbtPlugin("org.scalameta"    % "sbt-scalafmt" % "2.5.0")
