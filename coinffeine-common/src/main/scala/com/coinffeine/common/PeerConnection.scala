package com.coinffeine.common

case class PeerConnection(hostname: String, port: Int = PeerConnection.DefaultPort) {
  override def toString = s"coinffeine://$hostname:$port/"
}

object PeerConnection {

  val DefaultPort: Int = 4790

  private val ConnPattern = "coinffeine://(\\w+(?:\\.\\w+)*)(?::(\\d+))?/?".r

  /** Parse a connection string. If connection is not rightly represented, an
    * IllegalArgumentException is thrown.
    *
    * @param conn  The connection string to be parsed
    * @return      The parsed connection
    * @throws IllegalArgumentException  If connection format is invalid
    */
  def parse(conn: String): PeerConnection = conn match {
    case ConnPattern(hostname, portString) =>
      PeerConnection(hostname, parsePort(conn, portString))
    case _ => throw parseException(conn, "invalid format")
  }

  private def parsePort(conn: String, portString: String): Int = try {
    Option(portString).map(_.toInt).getOrElse(DefaultPort)
  } catch {
    case ex: NumberFormatException => throw parseException(conn, "invalid port format")
  }

  private def parseException(conn: String, message: String) =
    new IllegalArgumentException(s"cannot parse connection chain '$conn': $message")
}
