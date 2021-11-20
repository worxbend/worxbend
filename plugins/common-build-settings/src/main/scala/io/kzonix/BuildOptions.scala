package io.kzonix

import java.awt.geom.GeneralPath

object BuildOptions {

  sealed trait Environment

  object Environment {
    case object Production extends Environment
    case object Develop    extends Environment
  }

  sealed trait BuildStage
  sealed trait ContinuousIntegration extends BuildStage

  object BuildStage {
    case object LocalRun      extends BuildStage
    case object SelfHosted    extends ContinuousIntegration
    case object GithubActions extends ContinuousIntegration
  }

}
