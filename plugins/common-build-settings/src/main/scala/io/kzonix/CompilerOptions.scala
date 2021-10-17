package io.kzonix

object CompilerOptions {

  val StandardOptions: Seq[String] = Seq(
    "-explain",
    "-rewrite",
    "-indent",
    "-new-syntax",
    "-print-lines",
    "-print-tasty",
    "-doc-snapshot",
    "-explain-types"
  ) ++ Seq(
    "-unchecked",
    "-deprecation",
    "-feature",
    "-deprecation",
    "-language:implicitConversions",
    "-Xfatal-warnings",
    "-Xprint-inline",
    "-Xsemanticdb"
  ) ++ Seq(
    "-source",
    "future",
    "-color",
    "always"
  )

  val AdvancedOptions: Seq[String] = Seq(
    "-Xignore-scala2-macros",
    "-Xwiki-syntax",
    "-Ycheck-all-patmat",
    "-Ymacro-annotations",
    "-Ycheck-mods",
    "-Ycheck-reentrant",
    "-Ycook-comments",
    "-Ydebug-error",
    "-Yexplicit-nulls",
    "-Ykind-projector",
    "-Ysafe-init",
    "-Ycheck-all-patmat",
    "-Ydebug-error",
    "-Ydebug-flags",
    "-Ydetailed-stats",
    "-Yinstrument",
    "-Yinstrument-defs",
    "-Yforce-sbt-phases",
    "-Yprofile-enabled",
    "-Yprint-pos"
  )

}
