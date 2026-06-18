package io.kzonix.cetus.routes

import zio.http.Routes

trait AppRoutes[Env, Err] {
  def routes: Routes[Env, Err]
}
