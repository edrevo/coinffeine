package com.coinffeine.acceptance.peer

import scala.concurrent.Await
import scala.concurrent.duration._

import com.coinffeine.acceptance.broker.TestBrokerComponent
import com.coinffeine.client.peer.{PeerActor, PeerSupervisorActor}
import com.coinffeine.common.DefaultTcpPortAllocator
import com.coinffeine.common.network.NetworkComponent
import com.coinffeine.common.protocol.gateway.ProtoRpcMessageGateway

/** Cake-pattern factory of peers configured for GUI-less testing. */
trait TestPeerComponent
  extends PeerSupervisorActor.Component
  with PeerActor.Component
  with ProtoRpcMessageGateway.Component { this: TestBrokerComponent with NetworkComponent =>

  def buildPeer(): TestPeer = {
    val port = DefaultTcpPortAllocator.allocatePort()
    new TestPeer(peerSupervisorProps(port, broker.address), s"peer$port")
  }

  /** Loan pattern for a peer. It is guaranteed that the peers will be destroyed
    * even if the block throws exceptions.
    */
  def withPeer[T](block: TestPeer => T): T = {
    Await.ready(broker.start(), Duration.Inf)
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
