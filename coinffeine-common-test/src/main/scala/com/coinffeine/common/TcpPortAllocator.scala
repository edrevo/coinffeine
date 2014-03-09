package com.coinffeine.common

import java.net.ServerSocket
import scala.annotation.tailrec
import scala.util.control.NonFatal

/** Starting with an initial port, this class provide collision-free allocation of TCP ports
  * for integrated tests.
  */
class TcpPortAllocator(initialPort: Int) {

  /** Next port to probe */
  @volatile
  private var nextPort: Int = initialPort

  @tailrec
  final def allocatePort(): Int = synchronized {
    val currentPort = nextPort
    nextPort += 1
    try {
      new ServerSocket(currentPort).close()
      return currentPort
    } catch {
      case NonFatal(ex) =>
    }
    allocatePort()
  }

  def allocatePorts(amount: Int): Seq[Int] = synchronized {
    Seq.fill(amount)(allocatePort())
  }
}
