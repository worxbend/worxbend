package com.airgradient.proxy.domain

enum PollingResult:
  case Success(snapshot: CachedSnapshot)
  case Failure(error: String, errorType: String)
