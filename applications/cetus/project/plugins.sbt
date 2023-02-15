ThisBuild / scalaVersion    := "2.12.17"
ThisBuild / useSuperShell   := false
ThisBuild / autoStartServer := false

update / evictionWarningOptions := EvictionWarningOptions.empty

addDependencyTreePlugin

addSbtPlugin("com.github.sbt"   % "sbt-native-packager" % "1.9.15")
addSbtPlugin("com.eed3si9n"     % "sbt-assembly"        % "2.1.1")
addSbtPlugin("com.github.sbt"   % "sbt-git"             % "2.0.0")
addSbtPlugin("com.mayreh"       % "sbt-thank-you-stars" % "0.2")
addSbtPlugin("com.timushev.sbt" % "sbt-rewarn"          % "0.1.3")
addSbtPlugin("com.timushev.sbt" % "sbt-updates"         % "0.6.4")
addSbtPlugin("io.spray"         % "sbt-revolver"        % "0.9.1")
addSbtPlugin("org.scalameta"    % "sbt-scalafmt"        % "2.5.0")
