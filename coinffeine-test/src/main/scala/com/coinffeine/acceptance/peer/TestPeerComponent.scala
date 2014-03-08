package com.coinffeine.acceptance.peer

import com.coinffeine.acceptance.broker.TestBrokerComponent
import com.coinffeine.acceptance.mockpay.MockPayComponent
import com.coinffeine.client.peer.PeerActor
import com.coinffeine.common.currency.CurrencyCode
import com.coinffeine.common.protocol.Order

/** Cake-pattern factory of peers configured for gui-less testing. */
trait TestPeerComponent extends PeerActor.Component {
  this: TestBrokerComponent with MockPayComponent =>

  // TODO: create actual peers

  def buildPeer(): TestPeer = new TestPeer(peerActorProps)

  /** Loan pattern for a peer. It is guaranteed that the peers will be destroyed
    * even if the block throws exceptions.
    */
  def withPeer[T](block: TestPeer => T): T = {
    val peer = buildPeer()
    try {
      block(peer)
    } finally {
      peer.close()
    }
  }

  /** Loan pattern for a couple of peers. */
  def withPeerPair[T](block: (TestPeer, TestPeer) => T): T =
    withPeer(bob =>
      withPeer(sam =>
        block(bob, sam)
      ))
}
