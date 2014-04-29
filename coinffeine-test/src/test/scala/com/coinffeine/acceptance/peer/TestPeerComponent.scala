package com.coinffeine.acceptance.peer

import com.coinffeine.acceptance.IntegrationTestNetworkComponent
import com.coinffeine.client.peer.{PeerActor, PeerSupervisorActor}
import com.coinffeine.common.{DefaultTcpPortAllocator, PeerConnection}
import com.coinffeine.common.protocol.gateway.ProtoRpcMessageGateway
import com.coinffeine.common.protocol.serialization.DefaultProtocolSerializationComponent

/** Cake-pattern factory of peers configured for GUI-less testing. */
class TestPeerComponent
  extends PeerSupervisorActor.Component
  with PeerActor.Component
  with ProtoRpcMessageGateway.Component
  with DefaultProtocolSerializationComponent
  with IntegrationTestNetworkComponent {

  def buildPeer(brokerAddress: PeerConnection): TestPeer = {
    val port = DefaultTcpPortAllocator.allocatePort()
    new TestPeer(peerSupervisorProps(port, brokerAddress), s"peer$port")
  }
}
