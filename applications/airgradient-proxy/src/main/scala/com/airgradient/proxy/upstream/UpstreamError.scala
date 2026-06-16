package com.airgradient.proxy.upstream

enum UpstreamError:
  case Timeout(message: String)
  case Network(message: String)
  case BadStatus(statusCode: Int, message: String)
  case InvalidJson(message: String)
  case Interrupted(message: String)
  case Unexpected(message: String)

object UpstreamError:
  extension (e: UpstreamError)
    def errorType: String = e match
      case _: Timeout     => "Timeout"
      case _: Network     => "Network"
      case _: BadStatus   => "BadStatus"
      case _: InvalidJson => "InvalidJson"
      case _: Interrupted => "Interrupted"
      case _: Unexpected  => "Unexpected"

    def message: String = e match
      case Timeout(m)        => m
      case Network(m)        => m
      case BadStatus(_, m)   => m
      case InvalidJson(m)    => m
      case Interrupted(m)    => m
      case Unexpected(m)     => m
