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
    val exchangeInfo: ExchangeInfo[C],
    amountToCommit: BitcoinAmount,
    userWallet: Wallet) extends Handshake[C] {
  require(userWallet.hasKey(exchangeInfo.userKey),
    "User wallet does not contain the user's private key")

  override val commitmentTransaction: MutableTransaction =
    TransactionProcessor.createMultiSignedDeposit(
      userWallet, amountToCommit, Seq(exchangeInfo.counterpartKey, exchangeInfo.userKey),
      exchangeInfo.network
    )

  private val committedFunds = commitmentTransaction.getOutput(0)
  override val refundTransaction: MutableTransaction = {
    val tx = new MutableTransaction(exchangeInfo.network)
    tx.setLockTime(exchangeInfo.lockTime)
    tx.addInput(committedFunds).setSequenceNumber(0)
    tx.addOutput(committedFunds.getValue, exchangeInfo.userKey)
    ensureValidRefundTransaction(tx)
    tx
  }

  override def signCounterpartRefundTransaction(
      counterpartRefundTx: MutableTransaction): Try[TransactionSignature] = Try {
    ensureValidRefundTransaction(counterpartRefundTx)
    val connectedPubKeyScript = ScriptBuilder.createMultiSigOutputScript(
      2, List(exchangeInfo.userKey, exchangeInfo.counterpartKey))
    counterpartRefundTx.calculateSignature(
      0, exchangeInfo.userKey, connectedPubKeyScript, SigHash.ALL, false)
  }

  private def ensureValidRefundTransaction(refundTx: MutableTransaction) = {
    // TODO: Is this enough to ensure we can sign?
    require(refundTx.isTimeLocked)
    require(refundTx.getLockTime == exchangeInfo.lockTime)
    require(refundTx.getInputs.size == 1)
    require(refundTx.getConfidence.getConfidenceType == ConfidenceType.UNKNOWN)
  }

  override def validateRefundSignature(signature: TransactionSignature): Try[Unit] = Try {
    require(TransactionProcessor.isValidSignature(
      refundTransaction, index = 0, signature, exchangeInfo.counterpartKey,
      List(exchangeInfo.counterpartKey, exchangeInfo.userKey)))
  }
}
