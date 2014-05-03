package com.coinffeine.acceptance

import com.typesafe.config.ConfigFactory

import com.coinffeine.client.app.DefaultCoinffeineApp
import com.coinffeine.client.peer.{PeerActor, QuoteRequestActor}
import com.coinffeine.common.{DefaultTcpPortAllocator, PeerConnection}
import com.coinffeine.common.config.ConfigComponent
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.ProtoRpcMessageGateway
import com.coinffeine.common.protocol.serialization.DefaultProtocolSerializationComponent

/** Cake-pattern factory of peers configured for GUI-less testing. */
class TestCoinffeineApp(brokerAddress: PeerConnection) extends DefaultCoinffeineApp.Component
  with PeerActor.Component
  with QuoteRequestActor.Component
  with ProtoRpcMessageGateway.Component
  with DefaultProtocolSerializationComponent
  with IntegrationTestNetworkComponent
  with ConfigComponent
  with ProtocolConstants.DefaultComponent {

  override lazy val config = {
    val port = DefaultTcpPortAllocator.allocatePort()
    ConfigFactory.parseString(
      s"""
      |coinffeine.peer {
      |  host = "localhost"
      |  port = $port
      |}
      |coinffeine.broker.address = "$brokerAddress"
    """.stripMargin)
  }
}
