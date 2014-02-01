package com.bitwise.bitmarket.client.handshake

import scala.collection.JavaConversions._
import scala.util.Try

import com.google.bitcoin.core.{Transaction, TransactionOutput, Wallet}
import com.google.bitcoin.core.Transaction.SigHash
import com.google.bitcoin.crypto.TransactionSignature
import com.google.bitcoin.script.ScriptBuilder

import com.bitwise.bitmarket.client.Exchange
import com.bitwise.bitmarket.common.currency.BtcAmount
import com.google.bitcoin.core.TransactionConfidence.ConfidenceType

abstract class DefaultExchangeHandshake(
    val exchange: Exchange,
    inputFunds: Seq[TransactionOutput],
    amountToCommit: BtcAmount,
    userWallet: Wallet) extends ExchangeHandshake {
  require(userWallet.hasKey(exchange.userKey), "User wallet does not contain the user's private key")
  require(amountToCommit > BtcAmount(0), "Amount to commit must be greater than zero")

  private val totalInputFunds = inputFunds.map(funds => new BtcAmount(funds.getValue)).sum
  require(totalInputFunds > amountToCommit, "Input funds must cover the amount of funds to commit")

  override val commitmentTransaction: Transaction = {
    val tx = new Transaction(exchange.network)
    inputFunds.foreach(tx.addInput)
    tx.addOutput(
      (totalInputFunds - amountToCommit).asSatoshi,
      userWallet.getChangeAddress)
    tx.addOutput(
      amountToCommit.asSatoshi,
      ScriptBuilder.createMultiSigOutputScript(2, List(exchange.counterpartKey, exchange.userKey)))
    tx
  }
  private val committedFunds = commitmentTransaction.getOutput(1)
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
    val input = counterpartRefundTx.getInput(0)
    val connectedPubKeyScript = input.getConnectedOutput.getScriptPubKey
    counterpartRefundTx.calculateSignature(
      0, exchange.userKey, connectedPubKeyScript, SigHash.ALL, false)
  }

  private def ensureValidRefundTransaction(refundTx: Transaction) = {
    // TODO: Is this enough to ensure we can sign?
    require(refundTx.isTimeLocked)
    require(refundTx.getLockTime == exchange.lockTime)
    require(refundTx.getInputs.size == 1)
    require(refundTx.getConfidence.getConfidenceType == ConfidenceType.UNKNOWN)
    val connectedPubKeyScript = refundTx.getInput(0).getConnectedOutput.getScriptPubKey
    require(connectedPubKeyScript.isSentToMultiSig)
    val multiSigInfo = MultiSigInfo(connectedPubKeyScript)
    require(multiSigInfo.possibleKeys.size == 2)
    require(multiSigInfo.requiredKeyCount == 2)
    val expectedKeys = Set(exchange.counterpartKey, exchange.userKey)
    require(multiSigInfo.possibleKeys == expectedKeys)
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
