package com.coinffeine.client.handshake

import com.coinffeine.common._
import com.coinffeine.common.bitcoin._
import com.coinffeine.common.exchange._

class MockExchangeProtocol extends ExchangeProtocol {

  override def createHandshake(
      exchange: Exchange[_ <: FiatCurrency],
      role: Role,
      unspentOutputs: Seq[UnspentOutput],
      changeAddress: Address) = new MockHandshake(exchange, role)

  override def createMicroPaymentChannel(
      exchange: Exchange[_ <: FiatCurrency], role: Role, deposits: Deposits) = ???
}
