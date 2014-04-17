package com.coinffeine

import akka.actor.Props

import com.coinffeine.arbiter.{CommitmentValidation, HandshakeArbiterActor}
import com.coinffeine.broker.BrokerActor
import com.coinffeine.common.currency.CurrencyCode.{EUR, USD}
import com.coinffeine.common.network.MainNetComponent
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.ProtoRpcMessageGateway
import com.coinffeine.common.protocol.serialization.DefaultProtocolSerializationComponent
import com.coinffeine.common.system.ActorSystemBootstrap
import com.coinffeine.server.BrokerSupervisorActor

object Main extends ActorSystemBootstrap
  with BrokerSupervisorActor.Component
  with BrokerActor.Component
  with HandshakeArbiterActor.Component
  with ProtoRpcMessageGateway.Component
  with DefaultProtocolSerializationComponent
  with MainNetComponent
  with CommitmentValidation.Component
  with ProtocolConstants.Component {

  override val protocolConstants = ProtocolConstants.DefaultConstants
  val tradedCurrencies = Set(EUR, USD).map(_.currency)

  override protected val supervisorProps: Props = brokerSupervisorProps(tradedCurrencies)

  // TODO: implement a real CommitmentValidation
  override def commitmentValidation: CommitmentValidation = ???
}
