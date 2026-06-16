package com.airgradient.proxy.app

import com.airgradient.proxy.http.TapirServer
import com.airgradient.proxy.polling.PollingLoop
import ox.*

final class AppLifecycle(
  server:  TapirServer,
  polling: PollingLoop,
):

  def run(): Unit =
    supervised {
      val _ = forkUser { server.startAndWait() }
      val _ = forkUser { polling.run() }
    }
