package io.kzonix.reqflect.routes

import zio.http.HttpApp

trait AppRoutes[Env, Err] {
  def routes: HttpApp[Env, Err]
}
