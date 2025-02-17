Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / sbtVersion      := { cookiecutter.sbt_version }
ThisBuild / useSuperShell   := false
ThisBuild / autoStartServer := false

ThisBuild / update / evictionWarningOptions := EvictionWarningOptions.empty
update / evictionWarningOptions             := EvictionWarningOptions.empty

addDependencyTreePlugin

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.11.1")
addSbtPlugin("com.eed3si9n"   % "sbt-assembly"        % "2.3.1")
addSbtPlugin("org.scalameta"  % "sbt-scalafmt"        % "2.5.4")
