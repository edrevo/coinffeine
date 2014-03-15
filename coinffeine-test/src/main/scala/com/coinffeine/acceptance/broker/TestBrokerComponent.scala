package com.coinffeine.acceptance.broker

import com.coinffeine.common.DefaultTcpPortAllocator
import com.coinffeine.common.protocol.gateway.ProtoRpcMessageGateway
import com.coinffeine.server.ServerActor
import com.coinffeine.broker.BrokerActor

/** Cake-pattern factory of brokers configured for E2E testing */
trait TestBrokerComponent {

  lazy val broker: TestBroker = {
    val brokerComponent = new ServerActor.Component with BrokerActor.Component
      with ProtoRpcMessageGateway.Component
    val port = DefaultTcpPortAllocator.allocatePort()
    val args = Array("--port", port.toString)
    new TestBroker(brokerComponent.supervisorProps(args), port)
  }
}
