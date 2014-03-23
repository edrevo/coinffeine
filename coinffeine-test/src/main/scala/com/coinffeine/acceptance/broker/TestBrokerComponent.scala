package com.coinffeine.acceptance.broker

import com.coinffeine.broker.BrokerActor
import com.coinffeine.common.DefaultTcpPortAllocator
import com.coinffeine.common.network.NetworkComponent
import com.coinffeine.common.protocol.gateway.ProtoRpcMessageGateway
import com.coinffeine.common.protocol.serialization.DefaultProtocolSerializationComponent
import com.coinffeine.server.BrokerSupervisorActor

/** Cake-pattern factory of brokers configured for E2E testing */
trait TestBrokerComponent extends BrokerSupervisorActor.Component with BrokerActor.Component
  with ProtoRpcMessageGateway.Component with DefaultProtocolSerializationComponent {
  this: NetworkComponent =>

  lazy val broker: TestBroker = {
    val port = DefaultTcpPortAllocator.allocatePort()
    new TestBroker(brokerSupervisorProps(port), port)
  }
}
