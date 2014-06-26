package com.coinffeine.client.handshake

import com.coinffeine.common._
import com.coinffeine.common.bitcoin._
import com.coinffeine.common.exchange._

class MockExchangeProtocol extends ExchangeProtocol {

  override def createHandshake[C <: FiatCurrency](
      exchange: Exchange[C],
      role: Role,
      unspentOutputs: Seq[UnspentOutput],
      changeAddress: Address): Handshake[C] = new MockHandshake[C](exchange, role)
}
