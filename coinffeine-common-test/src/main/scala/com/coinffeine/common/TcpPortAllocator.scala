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
  final def allocatePort(): Int = {
    val currentPort = selectPort()
    if (canListenOn(currentPort)) currentPort
    else allocatePort()
  }

  def allocatePorts(amount: Int): Seq[Int] = Seq.fill(amount)(allocatePort())

  private def selectPort() = synchronized {
    val port = nextPort
    nextPort += 1
    port
  }

  private def canListenOn(port: Int): Boolean = try {
    new ServerSocket(port).close()
    true
  } catch {
    case NonFatal(ex) => false
  }
}
