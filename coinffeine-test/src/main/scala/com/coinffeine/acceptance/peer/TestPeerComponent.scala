package com.coinffeine.acceptance.peer

import com.coinffeine.acceptance.broker.TestBrokerComponent
import com.coinffeine.acceptance.mockpay.MockPayComponent
import com.coinffeine.common.currency.CurrencyCode
import com.coinffeine.common.protocol.Order

/** Cake-pattern factory of peers configured for gui-less testing
  *
  * @constructor
  */
trait TestPeerComponent { this: TestBrokerComponent with MockPayComponent =>

  // TODO: create actual peers

  def buildPeer(): TestPeer = new TestPeer {
    override def askForQuote(currency: CurrencyCode) {}
    override def placeOrder(order: Order) {}
    override def lastQuote = None
    override def shutdown() {}
  }

  /** Loan pattern for a peer. It is guaranteed that the peers will be destroyed
    * even if the block throws exceptions.
    */
  def withPeer[T](block: TestPeer => T): T = {
    val peer = buildPeer()
    try {
      block(peer)
    } finally {
      peer.shutdown()
    }
  }

  /** Loan pattern for a couple of peers. */
  def withPeerPair[T](block: (TestPeer, TestPeer) => T): T =
    withPeer(bob =>
      withPeer(sam =>
        block(bob, sam)
      ))
}
