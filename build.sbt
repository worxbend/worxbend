import BaseSettings.defaultSettings

// region global
/* >>----- BEGIN: Global Settings -----<< */
Global / onChangedBuildSource := ReloadOnSourceChanges
Global / startYear            := Some(2018)
/* >>----- END:   Global Settings  -----<< */

/* >>----- BEGIN: inThisBuild -----<< */
ThisBuild / scalaVersion := "3.4.2"
/* >>----- END:   inThisBuild -----<< */

lazy val commonSettings = defaultSettings
// endregion global

// region root
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
    // region aggregate-plugins
    `common-build-settings`,
    `project-builder`,
    `project-dependencies`,
    // endregion aggregate-plugins

    // region aggregate-apps
    `aeon`,
    `cetus`,
    `felis`,
    `inaya`,
    `meeter`,
    `nimblox`,
    `reqflect`,
    `sandbox`,
    `vex`,
    // endregion aggregate-apps

    // region aggregate-libs
    `worxflowr-core`,
    `describo`,
    `dscrbo`,
    // endregion aggregate-libs
  )
/* >>----- END: Root project -----<< */
// endregion root

// region scalafix
/* >>----- BEGIN: Scalafix -----<< */

/* ... */

/* >>----- END: Scalafix -----<< */
//endregion scalafix

// region plugins

/* >>-------------------------------<< */
lazy val `common-build-settings` = ProjectRef(
  base = file("plugins/common-build-settings"),
  id = "common-build-settings",
)
/* >>-------------------------------<< */


/* >>-------------------------------<< */
lazy val `project-builder`       = ProjectRef(
  base = file("plugins/project-builder"),
  id = "project-builder",
)
/* >>-------------------------------<< */


/* >>-------------------------------<< */
lazy val `project-dependencies`  = ProjectRef(
  base = file("plugins/project-dependencies"),
  id = "project-dependencies",
)
/* >>-------------------------------<< */


//endregion plugins

// region libs

/* >>-------------------------------<< */
lazy val `describo` = ProjectRef(
  base = file("libs/commons/describo"),
  id = "describo",
)
/* >>-------------------------------<< */

/* >>-------------------------------<< */
lazy val `dscrbo` = ProjectRef(
  base = file("libs/commons/dscrbo"),
  id = "dscrbo",
)
/* >>-------------------------------<< */

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

// endregion libs

// region apps

/* >>-------------------------------<< */
lazy val `aeon` = ProjectRef(
  base = file("applications/aeon"),
  id = "aeon",
)
/* >>-------------------------------<< */


/* >>-------------------------------<< */
lazy val `cetus` = ProjectRef(
  base = file("applications/cetus"),
  id = "cetus",
)
/* >>-------------------------------<< */


/* >>-------------------------------<< */
lazy val `felis` = ProjectRef(
  base = file("applications/felis"),
  id = "felis",
)
/* >>-------------------------------<< */


/* >>-------------------------------<< */
lazy val `inaya` = ProjectRef(
  base = file("applications/inaya"),
  id = "inaya",
)
/* >>-------------------------------<< */


/* >>-------------------------------<< */
lazy val `meeter` = ProjectRef(
  base = file("applications/meeter"),
  id = "meeter",
)
/* >>-------------------------------<< */


/* >>-------------------------------<< */
lazy val `nimblox` = ProjectRef(
  base = file("applications/nimblox"),
  id = "nimblox",
)
/* >>-------------------------------<< */


/* >>-------------------------------<< */
lazy val `reqflect` = ProjectRef(
  base = file("applications/reqflect"),
  id = "reqflect",
)
/* >>-------------------------------<< */


/* >>-------------------------------<< */
lazy val `sandbox` = ProjectRef(
  base = file("applications/sandbox"),
  id = "sandbox",
)
/* >>-------------------------------<< */


/* >>-------------------------------<< */
lazy val `vex` = ProjectRef(
  base = file("applications/void/vex"),
  id = "vex",
)
/* >>-------------------------------<< */

// endregion apps
