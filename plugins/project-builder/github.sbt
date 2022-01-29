ThisBuild / publishMavenStyle := true
ThisBuild / Test / publishArtifact := false
ThisBuild / pomIncludeRepository := { _ => false }

ThisBuild / publishConfiguration := publishConfiguration.value.withOverwrite(true)
ThisBuild / publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)

//mimaPreviousArtifacts := previousStableVersion.value.map(organization.value %% moduleName.value % _).toSet

githubOwner := "kzonix"
githubRepository := "kzonix-nova"
githubTokenSource := TokenSource.Or(
  TokenSource.Environment("GITHUB_TOKEN"),
  TokenSource.GitConfig("github.token")
)
