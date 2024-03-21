import BaseSettings.defaultSettings

/* >>----- BEGIN: Global Settings -----<< */
Global / onChangedBuildSource := ReloadOnSourceChanges
Global / startYear            := Some(2018)
/* >>----- END:   Global Settings  -----<< */

/* >>----- BEGIN: inThisBuild -----<< */
ThisBuild / scalaVersion := "3.3.3"
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
    `aeon`,
    `felis`,
    `cetus`,
    `meeter`,
    `reqflect`,
    `sandbox`,
    /* >>----------<< */
    `worxflowr-core`,
  )
/* >>----- END: Root project -----<< */

/* >>----- BEGIN: Scalafix -----<< */

/* ... */

/* >>----- END: Scalafix -----<< */

/* >>----- BEGIN: Plugins -----<< */
lazy val `common-build-settings` = ProjectRef(
  base = file("plugins/common-build-settings"),
  id = "common-build-settings",
)
/* >>-------------------------------<< */
lazy val `project-builder`       = ProjectRef(
  base = file("plugins/project-builder"),
  id = "project-builder",
)
/* >>-------------------------------<< */
lazy val `project-dependencies`  = ProjectRef(
  base = file("plugins/project-dependencies"),
  id = "project-dependencies",
)
/* >>----- END: Plugins -----<< */

/* >>----- BEGIN: Libraries -----<< */

/* >>-------------------------------<< */
lazy val `worxflowr-core` = ProjectRef(
  base = file("libs/worxflowr/worxflowr-core"),
  id = "worxflowr-core",
)
/* >>-------------------------------<< */

/* >>-------------------------------<< */
lazy val `worxflowr-scheduler` = ProjectRef(
  base = file("libs/worxflowr/worxflowr-scheduler"),
  id = "worxflowr-scheduler",
)
/* >>-------------------------------<< */

/* >>-------------------------------<< */
lazy val `worxflowr-server` = ProjectRef(
  base = file("libs/worxflowr/worxflowr-server"),
  id = "worxflowr-server",
)
/* >>-------------------------------<< */

/* >>----- END: Libraries -----<< */




/* >>----- BEGIN: Applications -----<< */

/* >>-------------------------------<< */
lazy val `aeon` = ProjectRef(
  base = file("applications/aeon"),
  id = "aeon",
)

/* >>-------------------------------<< */

/* >>-------------------------------<< */
lazy val `felis` = ProjectRef(
  base = file("applications/felis"),
  id = "felis",
)
/* >>-------------------------------<< */
lazy val `cetus` = ProjectRef(
  base = file("applications/cetus"),
  id = "cetus",
)
/* >>-------------------------------<< */

/* >>-------------------------------<< */
lazy val meeter = ProjectRef(
  base = file("applications/meeter"),
  id = "meeter",
)
/* >>-------------------------------<< */

/* >>-------------------------------<< */
lazy val `reqflect` = ProjectRef(
  base = file("applications/reqflect"),
  id = "reqflect",
)

lazy val `sandbox` = ProjectRef(
  base = file("applications/sandbox"),
  id = "sandbox",
)
/* >>-------------------------------<< */

/* >>----- END: Applications -----<< */

/* >>-------------------------------<< */
