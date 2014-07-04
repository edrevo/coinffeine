package com.coinffeine.client.app

import com.coinffeine.client.peer.{PeerActor, QuoteRequestActor}
import com.coinffeine.client.peer.orders.OrdersActor
import com.coinffeine.common.config.ConfigComponent
import com.coinffeine.common.network.MainNetComponent
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.ProtoRpcMessageGateway
import com.coinffeine.common.protocol.serialization.DefaultProtocolSerializationComponent

object ProductionCoinffeineApp {

  trait Component
    extends DefaultCoinffeineApp.Component
    with PeerActor.Component
    with ProtocolConstants.DefaultComponent
    with QuoteRequestActor.Component
    with OrdersActor.Component
    with ProtoRpcMessageGateway.Component
    with DefaultProtocolSerializationComponent
    with MainNetComponent
    with ConfigComponent
}
