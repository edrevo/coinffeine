package com.coinffeine.acceptance.broker

import com.coinffeine.common.PeerConnection

/** Cake-pattern factory of brokers configured for E2E testing */
trait TestBrokerComponent {

  // TODO: mix-in broker component and dependencies needed for broker creation
  lazy val broker: Broker = new Broker {
    override def close(): Unit = {}
    override def address = PeerConnection("fake")
  }
}
