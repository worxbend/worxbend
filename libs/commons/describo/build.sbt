ThisBuild / version := "0.1.0-SNAPSHOT"

/* >>------Versions-----<< */
val MagnoliaVersion  = "1.3.11"
val ScalaTestVersion = "3.2.19"

/* >>-------------------<< */

ThisBuild / scalaVersion := "3.6.3"

/* >>-------------------<< */

lazy val `describo` = (project in file("."))
  .settings(
    name := "describo",
    libraryDependencies ++= Seq(
      "com.softwaremill.magnolia1_3" %% "magnolia"  % MagnoliaVersion,
      "org.scalatest"                %% "scalatest" % ScalaTestVersion % "test",
    ),
  )
