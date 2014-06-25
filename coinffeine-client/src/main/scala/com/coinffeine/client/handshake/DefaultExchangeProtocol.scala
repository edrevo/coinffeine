package com.coinffeine.client.handshake

import com.coinffeine.common
import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin._
import com.coinffeine.common.exchange.{Role, UnspentOutput}
import com.coinffeine.common.exchange.impl.TransactionProcessor

/** Temporal fork of common.exchange.ExchangeProtocol to allow migrating the code */
class DefaultExchangeProtocol {

  @throws[IllegalArgumentException]("when funds are insufficient")
  def createHandshake[C <: FiatCurrency](exchange: common.exchange.Exchange[C],
                                         role: Role,
                                         unspentOutputs: Seq[UnspentOutput],
                                         changeAddress: Address): Handshake[C] = {
    val availableFunds = TransactionProcessor.valueOf(unspentOutputs.map(_.output))
    val depositAmount = role.myDepositAmount(exchange.amounts)
    require(availableFunds >= depositAmount,
      s"Expected deposit with $depositAmount ($availableFunds given)")
    val myDeposit = ImmutableTransaction {
      TransactionProcessor.createMultiSignedDeposit(
        unspentOutputs.map(_.toTuple), depositAmount, changeAddress,
        Seq(exchange.buyer.bitcoinKey, exchange.seller.bitcoinKey), exchange.parameters.network)
    }
    new DefaultHandshake(role, exchange, myDeposit)
  }
}

object DefaultExchangeProtocol {
  trait Component {
    lazy val exchangeProtocol = new DefaultExchangeProtocol
  }
}
