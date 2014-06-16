package com.coinffeine.common.exchange.impl

import com.google.bitcoin

import com.coinffeine.common._
import com.coinffeine.common.exchange.{Exchange, Handshake, Role}

case class DefaultExchange[C <: FiatCurrency]  (
  override val id: Exchange.Id,
  override val parameters: Exchange.Parameters,
  override val buyer: Exchange.PeerInfo[bitcoin.core.ECKey],
  override val seller: Exchange.PeerInfo[bitcoin.core.ECKey],
  override val broker: Exchange.BrokerInfo,
  override val amounts: Exchange.Amounts[C]) extends Exchange[C] {

  override type KeyPair = bitcoin.core.ECKey
  override type Transaction = bitcoin.core.Transaction
  override type TransactionSignature = bitcoin.crypto.TransactionSignature

  override def startHandshake(role: Role, myDeposit: Transaction): Handshake[C] = DefaultHandshake(
    exchange = this,
    myDeposit,
    myRefund = TransactionProcessor.createTransaction(
      inputs = Seq(myDeposit.getOutput(0)),
      outputs = Seq(role.me(this).bitcoinKey -> role.myDepositAmount(amounts))
    )
  )
}

object DefaultExchange {

  trait Component extends Exchange.Component {
    type KeyPair = bitcoin.core.ECKey

    override def createExchange[C <: FiatCurrency](id: Exchange.Id,
                                                   parameters: Exchange.Parameters,
                                                   buyer: Exchange.PeerInfo[KeyPair],
                                                   seller: Exchange.PeerInfo[KeyPair],
                                                   broker: Exchange.BrokerInfo,
                                                   amounts: Exchange.Amounts[C]) =
      DefaultExchange[C](id, parameters, buyer, seller, broker, amounts)
  }
}
