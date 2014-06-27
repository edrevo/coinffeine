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
  def createHandshake(exchange: Exchange[_ <: FiatCurrency],
                      role: Role,
                      unspentOutputs: Seq[UnspentOutput],
                      changeAddress: Address): Handshake

  /** Create a micro payment channel for an exchange given the deposit transactions and the
    * role to take.
    *
    * @param role       Role played in the protocol
    * @param exchange   Exchange description
    * @param deposits   Already compromised deposits for buyer and seller
    */
  def createMicroPaymentChannel(exchange: Exchange[_ <: FiatCurrency],
                                role: Role,
                                deposits: Deposits): MicroPaymentChannel
}

object ExchangeProtocol {
  trait Component {
    def exchangeProtocol: ExchangeProtocol
  }
}
