ThisBuild / scalaVersion    := "2.12.15"
ThisBuild / useSuperShell   := false
ThisBuild / autoStartServer := false

update / evictionWarningOptions := EvictionWarningOptions.empty

addDependencyTreePlugin

addCompilerPlugin("com.olegpy" %% "better-monadic-for"  % "0.3.1")
addSbtPlugin("com.github.sbt"   % "sbt-native-packager" % "1.9.9")
addSbtPlugin("com.eed3si9n"     % "sbt-assembly"        % "1.2.0")
addSbtPlugin("ch.epfl.scala"    % "sbt-scalafix"        % "0.10.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-git"             % "1.0.2")
addSbtPlugin("com.timushev.sbt" % "sbt-rewarn"          % "0.1.3")
addSbtPlugin("com.timushev.sbt" % "sbt-updates"         % "0.6.3")
addSbtPlugin("io.spray"         % "sbt-revolver"        % "0.9.1")
addSbtPlugin("org.scalameta"    % "sbt-scalafmt"        % "2.4.6")
addSbtPlugin("org.wartremover"  % "sbt-wartremover"     % "3.0.4")
