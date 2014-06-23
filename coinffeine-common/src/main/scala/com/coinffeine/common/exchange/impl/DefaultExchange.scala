package com.coinffeine.common.exchange.impl

import com.google.bitcoin

import com.coinffeine.common._
import com.coinffeine.common.bitcoin.ImmutableTransaction
import com.coinffeine.common.exchange.{Exchange, Role}

private[impl] case class DefaultExchange[C <: FiatCurrency]  (
  override val id: Exchange.Id,
  override val parameters: Exchange.Parameters[C],
  override val buyer: Exchange.PeerInfo[bitcoin.core.ECKey],
  override val seller: Exchange.PeerInfo[bitcoin.core.ECKey],
  override val broker: Exchange.BrokerInfo) extends Exchange[C] {

  override type KeyPair = bitcoin.core.ECKey
  override type Address = bitcoin.core.Address
  override type TransactionOutput = bitcoin.core.TransactionOutput
  override type Transaction = ImmutableTransaction
  override type TransactionSignature = bitcoin.crypto.TransactionSignature

  @throws[IllegalArgumentException]("when funds are insufficient")
  override def createHandshake(role: Role,
                               unspentOutputs: Seq[UnspentOutput],
                               changeAddress: Address) = {
    val availableFunds = TransactionProcessor.valueOf(unspentOutputs.map(_.output))
    val depositAmount = role.myDepositAmount(amounts)
    require(availableFunds >= depositAmount,
      s"Expected deposit with $depositAmount ($availableFunds given)")
    val myDeposit = ImmutableTransaction {
      TransactionProcessor.createMultiSignedDeposit(
        unspentOutputs.map(_.toTuple), depositAmount, changeAddress,
        Seq(buyer.bitcoinKey, seller.bitcoinKey), parameters.network)
    }
    DefaultHandshake(role, exchange = this, myDeposit)
  }
}

private[impl] object DefaultExchange {

  trait Component extends Exchange.Component {
    type KeyPair = bitcoin.core.ECKey

    override def createExchange[C <: FiatCurrency](id: Exchange.Id,
                                                   parameters: Exchange.Parameters[C],
                                                   buyer: Exchange.PeerInfo[KeyPair],
                                                   seller: Exchange.PeerInfo[KeyPair],
                                                   broker: Exchange.BrokerInfo) =
      DefaultExchange[C](id, parameters, buyer, seller, broker)
  }
}
