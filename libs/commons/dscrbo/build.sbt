ThisBuild / version := "0.1.0-SNAPSHOT"

/* >>------Versions-----<< */
val MagnoliaVersion  = "1.3.11"
val ScalaTestVersion = "3.2.19"

/* >>-------------------<< */

ThisBuild / scalaVersion := "3.6.4"

/* >>-------------------<< */

lazy val `dscrbo` = (project in file("."))
  .settings(
    name := "dscrbo",
    libraryDependencies ++= Seq(
      /* zero-dependencies */
      "org.scalatest" %% "scalatest" % ScalaTestVersion % "test"
    ),
  )
