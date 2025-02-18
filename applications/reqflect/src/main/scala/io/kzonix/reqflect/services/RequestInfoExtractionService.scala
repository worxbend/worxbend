package io.kzonix.reqflect.services

import zio.ZIO

import io.kzonix.reqflect.services.exceptions.ReqflectServiceException
import io.kzonix.reqflect.services.models.RequestInfo

trait RequestInfoExtractionService[Request] {

  def extractFrom(request: Request): ZIO[
    Any,
    ReqflectServiceException,
    RequestInfo,
  ]

}
