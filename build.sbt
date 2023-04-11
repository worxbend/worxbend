import BaseSettings._

/* -- BEGIN: Global Settings -- */
Global / onChangedBuildSource := ReloadOnSourceChanges
Global / startYear            := Some(2018)
/* -- END:   Global Settings  -- */

/* -- BEGIN: inThisBuild */
ThisBuild / scalaVersion := "3.2.2"
/* -- END:   inThisBuild */

lazy val commonSettings = defaultSettings

/* -- BEGIN: Root project -- */
lazy val `kzonix-mono` = (project in file("."))
  .enablePlugins(
    CommonBuildSettings,
    ProjectBuilder,
  )
  .settings(defaultSettings *)
  .settings(
    name := "kzonix-mono"
  )
  .aggregate(
    `common-build-settings`,
    `project-builder`,
    `project-dependencies`,
    `felis`,
    `cetus`,
    `meeter`,
    `reqflect`
  )
/* -- END: Root project -- */

/* -- BEGIN: Plugins -- */
lazy val `common-build-settings` = ProjectRef(
  file("plugins/common-build-settings"),
  "common-build-settings",
)
/* ------------------------- */
lazy val `project-builder`       = ProjectRef(
  file("plugins/project-builder"),
  "project-builder",
)
/* ------------------------- */
lazy val `project-dependencies`  = ProjectRef(
  file("plugins/project-dependencies"),
  "project-dependencies",
)
/* -- END: Plugins -- */

/* -- BEGIN: Applications -- */
lazy val `felis` = ProjectRef(
  file("applications/felis"),
  "felis",
)
/* ------------------------- */
lazy val `cetus` = ProjectRef(
  file("applications/cetus"),
  "cetus",
)
/* ------------------------- */

/* ------------------------- */
lazy val `meeter` = ProjectRef(
  file("applications/meeter"),
  "meeter",
)
/* ------------------------- */

/* ------------------------- */
lazy val `reqflect` = ProjectRef(
  file("applications/reqflect"),
  "reqflect",
)
/* ------------------------- */

/* -- END: Applications -- */
