package com.coinffeine.client.app

import akka.actor.{ActorSystem, Props}

import com.coinffeine.client.api._
import com.coinffeine.client.peer.PeerActor
import com.coinffeine.common.paymentprocessor.PaymentProcessor
import com.coinffeine.common.protocol.ProtocolConstants

/** Implements the coinffeine application API as an actor system.
  *
  * FIXME: partial API implementation
  */
class DefaultCoinffeineApp(peerProps: Props, override val protocolConstants: ProtocolConstants)
  extends CoinffeineApp {

  private val system = ActorSystem()
  private val peerRef = system.actorOf(peerProps, "peer")

  override val network = new DefaultCoinffeineNetwork(peerRef)

  override lazy val wallet = ???

  override val marketStats = new MarketStats {}

  override val paymentProcessors: Set[PaymentProcessor.Component] = Set.empty

  override def close(): Unit = system.shutdown()
}

object DefaultCoinffeineApp {
  trait Component extends CoinffeineAppComponent {
    this: PeerActor.Component with ProtocolConstants.Component =>

    override lazy val app = new DefaultCoinffeineApp(peerProps, protocolConstants)
  }
}
