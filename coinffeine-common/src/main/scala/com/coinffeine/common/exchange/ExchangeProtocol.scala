package com.coinffeine.common.exchange

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin._

trait ExchangeProtocol {

  /** Start a handshake for this exchange protocol.
    *
    * @param role            Role played in the protocol
    * @param exchange        Exchange description
    * @param unspentOutputs  Inputs for the deposit to create during the handshake
    * @param changeAddress   Address to return the excess of funds in unspentOutputs
    * @return                A new handshake
    */
  @throws[IllegalArgumentException]("when funds are insufficient")
  def createHandshake[C <: FiatCurrency](exchange: Exchange[C],
                                         role: Role,
                                         unspentOutputs: Seq[Exchange.UnspentOutput],
                                         changeAddress: Address): Handshake[C]
}

object ExchangeProtocol {
  trait Component {
    def exchangeProtocol: ExchangeProtocol
  }
}
