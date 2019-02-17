package de.gesellix.gradle.junixsocket.testkit

import groovy.util.logging.Slf4j
import org.newsclub.net.unix.AFUNIXSocket

@Slf4j
class AFUNIXSocketCheck {

  static boolean supported() {
    boolean supported = AFUNIXSocket.isSupported()
    log.lifecycle("AFUNIXSocket.isSupported(): ${supported}")

    AFUNIXSocket socket = AFUNIXSocket.newInstance()
    log.lifecycle("AFUNIXSocket.newInstance(): ${socket}")

    return supported && socket != null
  }
}
