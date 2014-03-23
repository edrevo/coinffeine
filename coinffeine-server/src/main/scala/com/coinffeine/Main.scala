package com.coinffeine

import com.coinffeine.broker.BrokerActor
import com.coinffeine.common.protocol.gateway.ProtoRpcMessageGateway
import com.coinffeine.server.BrokerSupervisorActor
import com.coinffeine.common.system.ActorSystemBootstrap
import akka.actor.Props

object Main extends ActorSystemBootstrap
  with BrokerSupervisorActor.Component
  with BrokerActor.Component
  with ProtoRpcMessageGateway.Component{

  override protected def supervisorProps(args: Array[String]): Props = {
    val cli = CommandLine.fromArgList(args)
    brokerSupervisorProps(cli.port)
  }
}
