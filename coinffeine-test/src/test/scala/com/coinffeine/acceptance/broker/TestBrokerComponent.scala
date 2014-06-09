package com.coinffeine.acceptance.broker

import com.google.bitcoin.core.Transaction

import com.coinffeine.acceptance.IntegrationTestNetworkComponent
import com.coinffeine.arbiter.{CommitmentValidation, HandshakeArbiterActor}
import com.coinffeine.broker.BrokerActor
import com.coinffeine.common.{FiatCurrency, DefaultTcpPortAllocator, PeerConnection}
import com.coinffeine.common.Currency.Euro
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.ProtoRpcMessageGateway
import com.coinffeine.common.protocol.serialization.DefaultProtocolSerializationComponent
import com.coinffeine.server.BrokerSupervisorActor

/** Cake-pattern factory of brokers configured for E2E testing */
class TestBrokerComponent(override val protocolConstants: ProtocolConstants)
  extends BrokerSupervisorActor.Component
  with BrokerActor.Component
  with HandshakeArbiterActor.Component
  with ProtoRpcMessageGateway.Component
  with DefaultProtocolSerializationComponent
  with CommitmentValidation.Component
  with ProtocolConstants.Component
  with IntegrationTestNetworkComponent {

  override def commitmentValidation = new CommitmentValidation {
    override def isValidCommitment(
        committer: PeerConnection, commitmentTransaction: Transaction): Boolean = ???
  }

  lazy val broker: TestBroker = {
    val port = DefaultTcpPortAllocator.allocatePort()
    val tradedCurrencies: Set[FiatCurrency] = Set(Euro)
    new TestBroker(brokerSupervisorProps(tradedCurrencies), port)
  }
}
