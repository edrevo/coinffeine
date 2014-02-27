package com.coinffeine.common

import java.net.ServerSocket
import java.io.IOException
import scala.util.Try

object NetworkTestUtils {

  /** Find a number of free TCP ports.
    *
    * Ports are found by opening probe server sockets.
    *
    * @param n Number of ports to find
    * @return  List of available ports
    * @throws RuntimeException When probe server sockets cannot be created or closed.
    */
  def findAvailableTcpPorts(n: Int): Seq[Int] = {
    val successfulSockets = Stream.continually(try {
      new ServerSocket(0)
    } catch {
      case ex: Exception => throw new RuntimeException("Cannot open probe socket", ex)
    }).take(n).toSeq
    successfulSockets.foreach(_.close)
    successfulSockets.map(_.getLocalPort)
  }
}
