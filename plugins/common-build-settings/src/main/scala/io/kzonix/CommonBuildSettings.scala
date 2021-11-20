package io.kzonix

import sbt._
import sbt.Keys._
import bloop.integrations.sbt.BloopDefaults
import bloop.integrations.sbt.BloopPlugin.autoImport._

object CommonBuildSettings extends AutoPlugin {

  object autoImport {

    val IntTest = config("it").extend(IntegrationTest)
    val Api     = config("api").extend(Default)

    val buildEnv   = settingKey[BuildOptions.Environment]("The build environment (`production`, `develop`)")
    val buildStage = settingKey[BuildOptions.BuildStage]("The build environment (`ci`, `gh-ci`, `other`)")

    lazy val IntegrationTestSettings =
      inConfig(IntTest)(
        Defaults.testSettings ++
          Seq(
            IntegrationTest / parallelExecution := false,
            IntegrationTest / scalaSource := baseDirectory.value / "src/it/scala"
          ) ++
          BloopDefaults.configSettings
      )

  }

  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] = super.projectSettings

  override def globalSettings: Seq[Def.Setting[_]] = super.globalSettings

  override def extraProjects: Seq[Project] = super.extraProjects

  override def buildSettings: Seq[Def.Setting[_]] =
    super.buildSettings

  override def trigger = super.trigger

  override def requires: Plugins = super.requires

  override val label: String = "CommonBuildSettings"

  override def toString(): String = super.toString() + "(KzonixPlugin)"

  override def projectConfigurations: Seq[Configuration] = super.projectConfigurations

  override def derivedProjects(proj: ProjectDefinition[_]) = super.derivedProjects(proj)

  override def allRequirements: PluginTrigger = super.allRequirements
}
