import scala.collection.immutable.Seq

object ScalacOptions {
  val Common: Seq[String] = Seq(
    "-deprecation",                 // emit warning and location for usages of deprecated APIs
    "-explain",                     // explain errors in more detail
    "-explain-types",               // explain type errors in more detail
    "-feature",                     // emit warning and location for usages of features that should be imported explicitly
    "-indent",                      // allow significant indentation.
    "-new-syntax",                  // require `then` and `do` in control expressions.
    "-print-lines",                 // show source code line numbers.
    "-unchecked",                   // enable additional warnings where generated code depends on assumptions
    "-Xfatal-warnings",             // fail the compilation if there are any warnings
    "-Xmigration:3.6",              // warn about constructs whose behavior may have changed since version
    "-Wunused:imports",             // warn about constructs whose behavior may have changed since version
    "-Wunused:locals",              // show warnings about unused things
    "-Wunused:privates",            // show warnings about unused things
    "-Wunused:params",              // show warnings about unused things
    "-Wunused:unsafe-warn-patvars", // show warnings about unused things
    "-Wunused:linted",
  )
}
