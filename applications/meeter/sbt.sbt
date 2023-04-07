Global / onChangedBuildSource := ReloadOnSourceChanges

Global / excludeLintKeys ++= Set(
  autoStartServer,
  turbo,
  evictionWarningOptions,
)

Test / parallelExecution := false

Test / testOptions += Tests.Argument(
  TestFrameworks.ScalaTest,
  "-oSD",
)

Test / turbo             := true

ThisBuild / autoStartServer        := false
ThisBuild / includePluginResolvers := true
ThisBuild / turbo                  := true

ThisBuild / watchBeforeCommand           := Watch.clearScreen
ThisBuild / watchTriggeredMessage        := Watch.clearScreenOnTrigger
ThisBuild / watchForceTriggerOnAnyChange := true
