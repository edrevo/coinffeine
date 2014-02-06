package com.bitwise.bitmarket

import com.bitwise.bitmarket.broker.BrokerActor
import com.bitwise.bitmarket.common.protocol.gateway.ProtoRpcMessageGateway
import com.bitwise.bitmarket.server.ServerActor
import com.bitwise.bitmarket.system.ActorSystemBootstrap

object Main extends ActorSystemBootstrap
  with ServerActor.Component
  with BrokerActor.Component
  with ProtoRpcMessageGateway.Component
