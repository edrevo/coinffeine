package com.coinffeine

import akka.actor.Props

import com.coinffeine.broker.BrokerActor
import com.coinffeine.common.protocol.gateway.ProtoRpcMessageGateway
import com.coinffeine.common.protocol.serialization.DefaultProtocolSerializationComponent
import com.coinffeine.common.system.ActorSystemBootstrap
import com.coinffeine.server.BrokerSupervisorActor

object Main extends ActorSystemBootstrap
  with BrokerSupervisorActor.Component
  with BrokerActor.Component
  with ProtoRpcMessageGateway.Component
  with DefaultProtocolSerializationComponent {

  override protected def supervisorProps(args: Array[String]): Props = {
    val cli = CommandLine.fromArgList(args)
    brokerSupervisorProps(cli.port)
  }
}
