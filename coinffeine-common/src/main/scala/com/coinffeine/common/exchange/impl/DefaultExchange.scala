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

  /** Start a handshake for this exchange.
    *
    * @param role       Role played in the handshake
    * @param myDeposit  Transaction in which the deposit will be compromised. The passed
    *                   object **should not be modified** once this method is called.
    * @return           A new handshake
    */
  override def startHandshake(role: Role, myDeposit: Transaction) = DefaultHandshake(
    exchange = this,
    myDeposit,
    myRefund = TransactionProcessor.createUnsignedTransaction(
      inputs = Seq(myDeposit.getOutput(0)),
      outputs = Seq(role.me(this).bitcoinKey -> role.myRefundAmount(amounts)),
      network = parameters.network,
      lockTime = Some(parameters.lockTime)
    ),
    herSignatureOfMyRefund = None
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
