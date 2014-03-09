package com.coinffeine.client.peer

import scala.util.control.NonFatal

import com.beust.jcommander._

import com.coinffeine.common.PeerConnection
import com.coinffeine.client.peer.PeerCommandLine.PeerConnectionConverter

class PeerCommandLine {
  @Parameter(names = Array("-p", "--port"), description = "Listen on this port")
  var port: Int = PeerCommandLine.DefaultPort

  @Parameter(
    names = Array("-b", "--broker"),
    description = "Address of the broker (i.e. coinffeine://host:port)",
    required = true,
    converter = classOf[PeerConnectionConverter]
  )
  var brokerAddress: PeerConnection = null
}

object PeerCommandLine {

  val DefaultPort = 8080

  def fromArgList(args: Seq[String]): PeerCommandLine = {
    val cli = new PeerCommandLine
    new JCommander(cli, args: _*)
    cli
  }

  private class PeerConnectionConverter extends IStringConverter[PeerConnection]{
    override def convert(value: String): PeerConnection = try {
      PeerConnection.parse(value)
    } catch {
      case NonFatal(e) => throw new ParameterException(s"Invalid broker address: '$value'")
    }
  }
}
