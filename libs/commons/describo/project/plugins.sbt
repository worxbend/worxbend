Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / sbtVersion      := "1.10.7"
ThisBuild / useSuperShell   := false
ThisBuild / autoStartServer := false

ThisBuild / update / evictionWarningOptions := EvictionWarningOptions.empty
update / evictionWarningOptions             := EvictionWarningOptions.empty

addDependencyTreePlugin
