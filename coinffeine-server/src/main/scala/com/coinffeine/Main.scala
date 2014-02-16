package com.coinffeine

import com.coinffeine.broker.BrokerActor
import com.coinffeine.common.protocol.gateway.ProtoRpcMessageGateway
import com.coinffeine.server.ServerActor
import com.coinffeine.system.ActorSystemBootstrap

object Main extends ActorSystemBootstrap
  with ServerActor.Component
  with BrokerActor.Component
  with ProtoRpcMessageGateway.Component
