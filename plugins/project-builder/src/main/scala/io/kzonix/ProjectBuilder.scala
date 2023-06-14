package io.kzonix

import ProjectDefaults.OrganizationSettings
import ProjectDefaults.ProjectSettings
import sbt._
import sbt.Keys._

object ProjectBuilder extends AutoPlugin {

  object autoImport {

    val organization     = settingKey[String]("The build environment (`production`, `develop`)")
    val organizationName = settingKey[String]("The build environment (`ci`, `gh-ci`, `other`)")

  }

  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] = super.projectSettings ++ ProjectSettings ++ OrganizationSettings

  override def globalSettings: Seq[Def.Setting[_]] = super.globalSettings

  override def extraProjects: Seq[Project] = super.extraProjects

  override def buildSettings: Seq[Def.Setting[_]] = super.buildSettings

  override def trigger = super.trigger

  override def requires: Plugins = super.requires

  override val label: String = "ProjectBuilder"

  override def toString(): String = super.toString() + "[KzonixPlugin]"

  override def projectConfigurations: Seq[Configuration] = super.projectConfigurations

  override def derivedProjects(proj: ProjectDefinition[_]) = super.derivedProjects(proj)

  override def allRequirements: PluginTrigger = super.allRequirements

}
