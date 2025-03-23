import sbt.*

logLevel                 := util.Level.Debug

resolvers ++= Resolver.sonatypeOssRepos("snapshots")

addSbtPlugin("ch.epfl.scala"  % "sbt-scalafix"        % "0.14.2")
addSbtPlugin("org.scalameta"  % "sbt-scalafmt"        % "2.5.4")
addSbtPlugin("com.github.sbt" % "sbt-dynver"          % "5.1.0")
addSbtPlugin("com.eed3si9n"   % "sbt-buildinfo"       % "0.13.1")
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.11.1")
