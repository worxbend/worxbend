package io.kzonix

object CompilerOptions {

  val StandardOptions: Seq[String] =
    Seq(
      "-explain",
      "-rewrite",
      "-indent",
      "-new-syntax",
      "-print-lines",
      "-print-tasty",
      "-doc-snapshot",
      "-explain-types",
    ) ++ Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-deprecation",
      "-language:implicitConversions",
      "-Xfatal-warnings",
      "-Xprint-inline",
      "-Xsemanticdb",
    ) ++ Seq(
      "-source",
      "future",
      "-color",
      "always",
    )

  val AdvancedOptions: Seq[String] = Seq.empty

}
