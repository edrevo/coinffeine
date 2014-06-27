package com.coinffeine.client.handshake

import com.coinffeine.client.exchange.MockProtoMicroPaymentChannel
import com.coinffeine.common._
import com.coinffeine.common.bitcoin._
import com.coinffeine.common.exchange._
import com.coinffeine.common.network.CoinffeineUnitTestNetwork

class MockExchangeProtocol extends ExchangeProtocol {

  override def createHandshake(
      exchange: Exchange[_ <: FiatCurrency],
      role: Role,
      unspentOutputs: Seq[UnspentOutput],
      changeAddress: Address) = new MockHandshake(exchange, role)

  override def createMicroPaymentChannel(
      exchange: Exchange[_ <: FiatCurrency], role: Role, deposits: Deposits) = ???

  override def createProtoMicroPaymentChannel(exchange: Exchange[_ <: FiatCurrency], role: Role,
                                              deposits: Deposits): ProtoMicroPaymentChannel =
    new MockProtoMicroPaymentChannel(exchange)
}

object MockExchangeProtocol {
  private val DummyDeposit = ImmutableTransaction(new MutableTransaction(CoinffeineUnitTestNetwork))
  val DummyDeposits = Deposits(DummyDeposit, DummyDeposit)
}
