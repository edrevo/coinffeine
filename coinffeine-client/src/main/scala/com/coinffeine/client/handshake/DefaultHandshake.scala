package com.coinffeine.client.handshake

import scala.collection.JavaConversions._
import scala.util.Try

import com.google.bitcoin.core.Transaction.SigHash
import com.google.bitcoin.core.TransactionConfidence.ConfidenceType
import com.google.bitcoin.script.ScriptBuilder

import com.coinffeine.client.ExchangeInfo
import com.coinffeine.common.{BitcoinAmount, FiatCurrency}
import com.coinffeine.common.bitcoin._
import com.coinffeine.common.exchange.impl.TransactionProcessor

abstract class DefaultHandshake[C <: FiatCurrency](
    exchangeInfo: ExchangeInfo[C],
    amountToCommit: BitcoinAmount,
    userWallet: Wallet) extends Handshake[C] {
  require(userWallet.hasKey(exchangeInfo.user.bitcoinKey),
    "User wallet does not contain the user's private key")

  override val exchange = exchangeInfo.exchange
  override val role = exchangeInfo.role

  override val commitmentTransaction = ImmutableTransaction(
    TransactionProcessor.createMultiSignedDeposit(
      userWallet,
      amountToCommit,
      Seq(exchangeInfo.counterpart.bitcoinKey, exchangeInfo.user.bitcoinKey),
      exchange.parameters.network
    )
  )

  override val unsignedRefundTransaction = {
    val committedFunds = commitmentTransaction.get.getOutput(0)
    val tx = new MutableTransaction(exchange.parameters.network)
    tx.setLockTime(exchange.parameters.lockTime)
    tx.addInput(committedFunds).setSequenceNumber(0)
    tx.addOutput(committedFunds.getValue, exchangeInfo.user.bitcoinKey)
    ensureValidRefundTransaction(tx)
    ImmutableTransaction(tx)
  }

  override def signCounterpartRefundTransaction(
      counterpartRefundTx: MutableTransaction): Try[TransactionSignature] = Try {
    ensureValidRefundTransaction(counterpartRefundTx)
    val connectedPubKeyScript = ScriptBuilder.createMultiSigOutputScript(
      2, List(exchangeInfo.user.bitcoinKey, exchangeInfo.counterpart.bitcoinKey))
    counterpartRefundTx.calculateSignature(
      0, exchangeInfo.user.bitcoinKey, connectedPubKeyScript, SigHash.ALL, false)
  }

  private def ensureValidRefundTransaction(refundTx: MutableTransaction) = {
    // TODO: Is this enough to ensure we can sign?
    require(refundTx.isTimeLocked)
    require(refundTx.getLockTime == exchange.parameters.lockTime)
    require(refundTx.getInputs.size == 1)
    require(refundTx.getConfidence.getConfidenceType == ConfidenceType.UNKNOWN)
  }

  override def validateRefundSignature(signature: TransactionSignature): Try[Unit] = Try {
    require(TransactionProcessor.isValidSignature(
      unsignedRefundTransaction.get, index = 0, signature, exchangeInfo.counterpart.bitcoinKey,
      List(exchangeInfo.counterpart.bitcoinKey, exchangeInfo.user.bitcoinKey)))
  }
}
