package io.kzonix.reqflect.services

import io.kzonix.reqflect.services.exceptions.ReqflectServiceException
import io.kzonix.reqflect.services.models.RequestInfo
import zio.ZIO

trait RequestInfoExtractionService[Request] {
  def extractFrom(request: Request): ZIO[Any, ReqflectServiceException, RequestInfo]
}

  
