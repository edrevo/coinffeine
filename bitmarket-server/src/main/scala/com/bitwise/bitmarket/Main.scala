package com.bitwise.bitmarket

import com.bitwise.bitmarket.broker.DefaultBrokerActor
import com.bitwise.bitmarket.protorpc.DefaultProtobufServerActor
import com.bitwise.bitmarket.server.ServerActor
import com.bitwise.bitmarket.system.ActorSystemBootstrap

object Main extends ActorSystemBootstrap
  with ServerActor.Component
  with DefaultProtobufServerActor.Component
  with DefaultBrokerActor.Component
