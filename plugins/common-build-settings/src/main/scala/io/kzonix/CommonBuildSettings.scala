package io.kzonix

import bloop.integrations.sbt.BloopDefaults
import sbt.Def
import sbt.*
import CompilerOptions.*
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.docker.DockerPlugin
import de.heikoseeberger.sbtheader.AutomateHeaderPlugin
import sbt.Keys.*
import sbt.plugins.JvmPlugin

object CommonBuildSettings extends AutoPlugin {

  object autoImport {

    val IntTest = config("it").extend(IntegrationTest)

    val buildEnv   = settingKey[BuildOptions.Environment]("The build environment (`production`, `develop`)")
    val buildStage = settingKey[BuildOptions.BuildStage]("The build environment (`ci`, `gh-ci`, `other`)")

    lazy val IntegrationTestSettings: Seq[Def.Setting[?]] =
      inConfig(IntTest)(
        Defaults.testSettings ++
          Seq(
            IntegrationTest / parallelExecution := false,
            IntegrationTest / scalaSource := baseDirectory.value / "src/it/scala"
          ) ++
          BloopDefaults.configSettings
      )

    val printPluginDependencies = taskKey[Unit]("It prints all the plugins")
  }

  override def projectSettings: Seq[Def.Setting[?]] =
    super.projectSettings ++ Seq(
      ThisBuild / scalacOptions := StandardOptions ++ AdvancedOptions
    )

  override def globalSettings: Seq[Def.Setting[?]] = super.globalSettings

  override def extraProjects: Seq[Project] = super.extraProjects

  override def buildSettings: Seq[Def.Setting[?]] =
    super.buildSettings

  /** !revise-me: consider `allRequirements` trigger. The build user still needs to include this plugin in
    * project/plugins.sbt, but it is no longer needed to be included in build.sbt. This becomes more interesting when
    * you do specify a plugin with requirements. Let’s modify the SbtLessPlugin so that it depends on another plugin.
    * This allows plugins to silently, and correctly, extend existing plugins with more features. It also can help
    * remove the burden of ordering from the user, allowing the plugin authors greater freedom and power when providing
    * feature for their users.
    */
  override def trigger = super.trigger

  /** The requires method returns a value of type Plugins, which is a DSL for constructing the dependency list. The
    * requires method typically contains one of the following values:
    *
    *   - empty (No plugins)
    *   - other auto plugins
    *   - && operator (for defining multiple dependencies)
    */
  override def requires: Plugins = super.requires

  override val label: String = "CommonBuildSettings"

  override def toString(): String = s"[${ super.toString() }:KzonixPlugin]"

  override def projectConfigurations: Seq[Configuration] = super.projectConfigurations

  override def derivedProjects(proj: ProjectDefinition[?]) = super.derivedProjects(proj)

  override def allRequirements: PluginTrigger = super.allRequirements

  lazy val printRequiredPlugins = Def.task {

    println("")
  }

}
