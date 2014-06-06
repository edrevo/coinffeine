package com.coinffeine.client.peer

import akka.actor.Props

/** Topmost actor on a peer node. */
object PeerActor {

  /** Start peer connection to the network. The sender of this message will receive either
    * a [[Connected]] or [[ConnectionFailed]] message in response. */
  case object Connect
  case object Connected
  case class ConnectionFailed(cause: Throwable)

  trait Component {
    def peerProps: Props
  }
}
