package io.kzonix

object CompilerOptions {

  val StandardOptions: Seq[String] =
    Seq(
      "-rewrite",
      "-indent",
      "-new-syntax",
      "-explain-types",
      "-explain",
    ) ++ Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-Xfatal-warnings",
      "-Xsemanticdb",
    )

  val AdvancedOptions: Seq[String] = Seq.empty

}
