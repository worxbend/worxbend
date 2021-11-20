import sbt.Keys._
import sbt.Resolver
import sbt.Setting
import sbt.url

import java.text.SimpleDateFormat
import java.util.Calendar
import scala.language.postfixOps

object BaseSettings {

  private val standardOptions: Seq[String] = Seq(
    "-color always",
    "-source:3.0-migration",
    "-explain",
    "-indent",
    "-new-syntax",
    "-print-lines",
    "-print-lines",
    "-print-tasty",
    "-doc-snapshot"
  )

  private val advancedOptions: Seq[String] = Seq(
    "-Xignore-scala2-macros",
    "-Xwiki-syntax",
    "-Ycheck-all-patmat",
    "-Ycheck-mods",
    "-Ycheck-reentrant",
    "-Ycook-comments",
    "-Ydebug-error"
  )

  val defaultSettings: Seq[Setting[_]] = Seq(
    versionScheme := Some("semver-spec"),
    startYear := Some(2020),
    organization := "io.kzonix",
    organizationName := "Kzonix Projects",
    version := Utils.Versions.version(),
    scalaVersion := "3.1.0",
    scalacOptions := Seq[String](
      "-unchecked",
      "-deprecation",
      "-feature",
      "-Ymacro-annotations"
    ) ++ standardOptions ++ advancedOptions,
    description := "N/A",
    licenses += ("MIT", url("https://www.gnu.org/licenses/gpl-2.0.html")),
    resolvers ++= Seq(
      Resolver.mavenLocal,
      Resolver.mavenCentral,
      Resolver.sonatypeRepo("snapshots")
    )
    // ThisBuild / credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
    // TODO: Add `publishTo` config for self-hosted sonatype-repo
  )

  object Utils {

    // TODO: Revise version management:
    //  CI pipeline should trigger build with appropriate parameters (snapshot vs release)
    //   - An appropriate environment variable should be provided to set version per CI build.
    //   - Implementation of versioning should be done according to VCS changelog and metadata from sonatype repo (previous version should be provided tp )
    object Versions {

      def version(): String = {
        val date: java.util.Date = Calendar.getInstance.getTime
        new SimpleDateFormat("yy.MM.dd.HHmmssSSS").format(date)
      }

      def milestone(num: Int): String =
        version().concat(s"M$num")

      def generalAvailability: String =
        version().concat("-GA")

      def beta(num: Int): String =
        version().concat("-%04db".format(num))
    }

  }

}
