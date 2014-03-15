package com.coinffeine.acceptance.peer

import com.coinffeine.acceptance.broker.TestBrokerComponent
import com.coinffeine.acceptance.mockpay.MockPayComponent
import com.coinffeine.client.peer.{PeerSupervisorActor, PeerActor}
import com.coinffeine.common.DefaultTcpPortAllocator
import com.coinffeine.common.protocol.gateway.ProtoRpcMessageGateway

/** Cake-pattern factory of peers configured for gui-less testing. */
trait TestPeerComponent { this: TestBrokerComponent with MockPayComponent =>

  def buildPeer(): TestPeer = {
    val peerComponent = new PeerSupervisorActor.Component with PeerActor.Component
      with ProtoRpcMessageGateway.Component
    val port = DefaultTcpPortAllocator.allocatePort()
    val args = Array("--port", port.toString, "--broker", broker.address.toString)
    new TestPeer(peerComponent.supervisorProps(args), s"peer$port")
  }

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
