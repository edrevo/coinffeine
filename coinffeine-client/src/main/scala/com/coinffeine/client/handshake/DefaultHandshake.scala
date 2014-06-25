package com.coinffeine.client.handshake

import scala.collection.JavaConversions._
import scala.util.Try

import com.google.bitcoin.core.Transaction.SigHash
import com.google.bitcoin.script.ScriptBuilder

import com.coinffeine.client.ExchangeInfo
import com.coinffeine.common.{BitcoinAmount, Currency, FiatCurrency}
import com.coinffeine.common.bitcoin._
import com.coinffeine.common.exchange.Handshake.InvalidRefundTransaction
import com.coinffeine.common.exchange.impl.{TransactionProcessor, UnsignedRefundTransaction}

class DefaultHandshake[C <: FiatCurrency](
    exchangeInfo: ExchangeInfo[C], userWallet: Wallet) extends Handshake[C] {
  require(userWallet.hasKey(exchangeInfo.user.bitcoinKey),
    "User wallet does not contain the user's private key")

  override val exchange = exchangeInfo.exchange
  override val role = exchangeInfo.role

  override val myDeposit = ImmutableTransaction(
    TransactionProcessor.createMultiSignedDeposit(
      userWallet,
      role.myDepositAmount(exchange.amounts),
      Seq(exchangeInfo.counterpart.bitcoinKey, exchangeInfo.user.bitcoinKey),
      exchange.parameters.network
    )
  )

  override val myUnsignedRefund = UnsignedRefundTransaction(
    deposit = myDeposit,
    outputKey = role.me(exchange).bitcoinKey,
    outputAmount = role.myRefundAmount(exchange.amounts),
    lockTime = exchange.parameters.lockTime,
    network = exchange.parameters.network
  )

  override def signHerRefund(counterpartRefundTx: ImmutableTransaction): TransactionSignature = {
    ensureValidRefundTransaction(counterpartRefundTx, role.herRefundAmount(exchange.amounts))
    val connectedPubKeyScript = ScriptBuilder.createMultiSigOutputScript(
      2, List(exchangeInfo.user.bitcoinKey, exchangeInfo.counterpart.bitcoinKey))
    counterpartRefundTx.get.calculateSignature(
      0, exchangeInfo.user.bitcoinKey, connectedPubKeyScript, SigHash.ALL, false)
  }

  private def ensureValidRefundTransaction(tx: ImmutableTransaction,
                                           expectedAmount: BitcoinAmount) = {
    def requireProperty(cond: MutableTransaction => Boolean, cause: String): Unit = {
      if (!cond(tx.get)) throw new InvalidRefundTransaction(tx, cause)
    }
    def validateAmount(tx: MutableTransaction): Boolean = {
      val amount = Currency.Bitcoin.fromSatoshi(tx.getOutput(0).getValue)
      amount == expectedAmount
    }
    // TODO: Is this enough to ensure we can sign?
    requireProperty(_.isTimeLocked, "lack a time lock")
    requireProperty(_.getLockTime == exchange.parameters.lockTime, "wrong time lock")
    requireProperty(_.getInputs.size == 1, "should have one input")
    requireProperty(validateAmount, "wrong refund amount")
  }

  override def validateRefundSignature(signature: TransactionSignature): Try[Unit] = Try {
    require(TransactionProcessor.isValidSignature(
      myUnsignedRefund.get, index = 0, signature, exchangeInfo.counterpart.bitcoinKey,
      List(exchangeInfo.counterpart.bitcoinKey, exchangeInfo.user.bitcoinKey)))
  }
}
