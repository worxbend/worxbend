import BaseSettings.defaultSettings

/* >>----- BEGIN: Global Settings -----<< */
Global / onChangedBuildSource := ReloadOnSourceChanges
Global / startYear            := Some(2018)
/* >>----- END:   Global Settings  -----<< */

/* >>----- BEGIN: inThisBuild -----<< */
ThisBuild / scalaVersion := "3.2.2"
/* >>----- END:   inThisBuild -----<< */

lazy val commonSettings = defaultSettings

/* >>----- BEGIN: Root project -----<< */
lazy val `worxbend` = (project in file("."))
  .enablePlugins(
    CommonBuildSettings,
    ProjectBuilder,
  )
  .settings(defaultSettings *)
  .settings(
    name := "worxbend"
  )
  .aggregate(
    /* >>----------<< */
    /*`scalafix-rules` ,*/
    /* >>----------<< */
    `common-build-settings`,
    `project-builder`,
    `project-dependencies`,
    /* >>----------<< */
    `felis`,
    `cetus`,
    `meeter`,
    `reqflect`,
    /* >>----------<< */
    `worxflowr-core`,
  )
/* >>----- END: Root project -----<< */

/* >>----- BEGIN: Scalafix -----<< */

/* ... */

/* >>----- END: Scalafix -----<< */

/* >>----- BEGIN: Plugins -----<< */
lazy val `common-build-settings` = ProjectRef(
  file("plugins/common-build-settings"),
  "common-build-settings",
)
/* >>-------------------------------<< */
lazy val `project-builder`       = ProjectRef(
  file("plugins/project-builder"),
  "project-builder",
)
/* >>-------------------------------<< */
lazy val `project-dependencies`  = ProjectRef(
  file("plugins/project-dependencies"),
  "project-dependencies",
)
/* >>----- END: Plugins -----<< */

/* >>----- BEGIN: Libraries -----<< */

/* >>-------------------------------<< */
lazy val `worxflowr-core` = ProjectRef(
  file("libs/worxflowr/worxflowr-core"),
  "worxflowr-core",
)
/* >>-------------------------------<< */

/* >>-------------------------------<< */
lazy val `worxflowr-scheduler` = ProjectRef(
  file("libs/worxflowr/worxflowr-scheduler"),
  "worxflowr-scheduler",
)
/* >>-------------------------------<< */

/* >>-------------------------------<< */
lazy val `worxflowr-server` = ProjectRef(
  file("libs/worxflowr/worxflowr-server"),
  "worxflowr-server",
)
/* >>-------------------------------<< */

/* >>----- END: Libraries -----<< */

/* >>----- BEGIN: Applications -----<< */

/* >>-------------------------------<< */
lazy val `felis` = ProjectRef(
  file("applications/felis"),
  "felis",
)
/* >>-------------------------------<< */
lazy val `cetus` = ProjectRef(
  file("applications/cetus"),
  "cetus",
)
/* >>-------------------------------<< */

/* >>-------------------------------<< */
lazy val meeter = ProjectRef(
  file("applications/meeter"),
  "meeter",
)
/* >>-------------------------------<< */

/* >>-------------------------------<< */
lazy val `reqflect` = ProjectRef(
  file("applications/reqflect"),
  "reqflect",
)
/* >>-------------------------------<< */

/* >>----- END: Applications -----<< */

/* >>-------------------------------<< */
