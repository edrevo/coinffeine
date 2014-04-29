package com.coinffeine.acceptance.peer

import com.typesafe.config.ConfigFactory

import com.coinffeine.acceptance.IntegrationTestNetworkComponent
import com.coinffeine.client.peer.{PeerActor, PeerSupervisorActor}
import com.coinffeine.common.{DefaultTcpPortAllocator, PeerConnection}
import com.coinffeine.common.config.ConfigComponent
import com.coinffeine.common.protocol.gateway.ProtoRpcMessageGateway
import com.coinffeine.common.protocol.serialization.DefaultProtocolSerializationComponent

/** Cake-pattern factory of peers configured for GUI-less testing. */
class TestPeerComponent(brokerAddress: PeerConnection)
  extends PeerSupervisorActor.Component
  with PeerActor.Component
  with ProtoRpcMessageGateway.Component
  with DefaultProtocolSerializationComponent
  with IntegrationTestNetworkComponent
  with ConfigComponent {

  lazy val port = DefaultTcpPortAllocator.allocatePort()

  override lazy val config = ConfigFactory.parseString(
   s"""
      |coinffeine.peer {
      |  host = "localhost"
      |  port = $port
      |}
      |coinffeine.broker.address = "$brokerAddress"
    """.stripMargin)

  val peer = new TestPeer(peerSupervisorProps, s"peer$port")
}
