package io.kzonix.reqflect.services.exceptions

sealed trait ReqflectServiceException

object ReqflectServiceException:

  case class GeneralException(message: String) extends ReqflectServiceException

  case class NoContainerInformationAvailable(message: String) extends ReqflectServiceException
