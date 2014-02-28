package com.coinffeine.client.handshake

import scala.collection.JavaConversions._
import scala.language.postfixOps
import scala.util.Try

import com.google.bitcoin.core.{Transaction, Wallet}
import com.google.bitcoin.core.Transaction.SigHash
import com.google.bitcoin.crypto.TransactionSignature
import com.google.bitcoin.script.ScriptBuilder

import com.coinffeine.client.Exchange
import com.coinffeine.common.currency.BtcAmount
import com.coinffeine.common.currency.BtcAmount.Implicits._
import com.google.bitcoin.core.TransactionConfidence.ConfidenceType

abstract class DefaultExchangeHandshake(
    val exchange: Exchange,
    amountToCommit: BtcAmount,
    userWallet: Wallet) extends ExchangeHandshake {
  require(userWallet.hasKey(exchange.userKey),
    "User wallet does not contain the user's private key")
  require(amountToCommit > (0 bitcoins), "Amount to commit must be greater than zero")

  private val inputFunds = {
    val inputFundCandidates = userWallet.calculateAllSpendCandidates(true)
    val necessaryInputCount = inputFundCandidates.view
      .scanLeft(0 bitcoins)((accum, output) => accum + BtcAmount(output.getValue))
      .takeWhile(_ < amountToCommit)
      .length
    inputFundCandidates.take(necessaryInputCount)
  }
  private val totalInputFunds = inputFunds.map(funds => BtcAmount(funds.getValue)).sum
  require(totalInputFunds >= amountToCommit,
    "Input funds must cover the amount of funds to commit")

  override val commitmentTransaction: Transaction = {
    val tx = new Transaction(exchange.network)
    inputFunds.foreach(tx.addInput)
    val changeAmount = totalInputFunds - amountToCommit
    require(changeAmount >= (0 bitcoins))
    tx.addOutput(
      amountToCommit.asSatoshi,
      ScriptBuilder.createMultiSigOutputScript(2, List(exchange.counterpartKey, exchange.userKey)))
    if (changeAmount > (0 bitcoins)) {
      tx.addOutput(
        (totalInputFunds - amountToCommit).asSatoshi,
        userWallet.getChangeAddress)
    }
    tx.signInputs(SigHash.ALL, userWallet)
    tx
  }
  private val committedFunds = commitmentTransaction.getOutput(0)
  override val refundTransaction: Transaction = {
    val tx = new Transaction(exchange.network)
    tx.setLockTime(exchange.lockTime)
    tx.addInput(committedFunds).setSequenceNumber(0)
    tx.addOutput(committedFunds.getValue, exchange.userKey)
    ensureValidRefundTransaction(tx)
    tx
  }

  override def signCounterpartRefundTransaction(
      counterpartRefundTx: Transaction): Try[TransactionSignature] = Try {
    ensureValidRefundTransaction(counterpartRefundTx)
    val connectedPubKeyScript = ScriptBuilder.createMultiSigOutputScript(
      2, List(exchange.userKey, exchange.counterpartKey))
    counterpartRefundTx.calculateSignature(
      0, exchange.userKey, connectedPubKeyScript, SigHash.ALL, false)
  }

  private def ensureValidRefundTransaction(refundTx: Transaction) = {
    // TODO: Is this enough to ensure we can sign?
    require(refundTx.isTimeLocked)
    require(refundTx.getLockTime == exchange.lockTime)
    require(refundTx.getInputs.size == 1)
    require(refundTx.getConfidence.getConfidenceType == ConfidenceType.UNKNOWN)
  }

  override def validateRefundSignature(signature: TransactionSignature): Try[Unit] = Try {
    val input = refundTransaction.getInput(0)
    require(exchange.counterpartKey.verify(
      refundTransaction.hashForSignature(
        0,
        input.getConnectedOutput.getScriptPubKey,
        SigHash.ALL,
        false),
      signature))
  }
}
