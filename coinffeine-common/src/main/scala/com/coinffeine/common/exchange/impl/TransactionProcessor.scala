package com.coinffeine.common.exchange.impl

import scala.collection.JavaConversions._

import com.google.bitcoin.core.Transaction.SigHash
import com.google.bitcoin.script.ScriptBuilder

import com.coinffeine.common.{BitcoinAmount, Currency}
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin._

/** This trait encapsulates the transaction processing actions. */
object TransactionProcessor {

  def createMultiSignedDeposit(unspentOutputs: Seq[(MutableTransactionOutput, KeyPair)],
                               amountToCommit: BitcoinAmount,
                               changeAddress: Address,
                               requiredSignatures: Seq[PublicKey],
                               network: Network): MutableTransaction = {
    require(amountToCommit.isPositive, "Amount to commit must be greater than zero")

    val inputFunds = unspentOutputs.map(_._1)
    val totalInputFunds = valueOf(inputFunds)
    require(totalInputFunds >= amountToCommit,
      "Input funds must cover the amount of funds to commit")

    val tx = new MutableTransaction(network)
    inputFunds.foreach(tx.addInput)
    addMultisignOutput(tx, amountToCommit, requiredSignatures)
    addChangeOutput(tx, totalInputFunds, amountToCommit, changeAddress)

    for (((output, signingKey), index) <- unspentOutputs.zipWithIndex) {
      val input = tx.getInput(index)
      val connectedScript = input.getOutpoint.getConnectedOutput.getScriptBytes
      val signature = tx.calculateSignature(index, signingKey, null, connectedScript, SigHash.ALL, false)
      input.setScriptSig(ScriptBuilder.createInputScript(signature, signingKey))
    }
    tx
  }

  def collectFunds(userWallet: Wallet, amount: BitcoinAmount): Set[MutableTransactionOutput] = {
    val inputFundCandidates = userWallet.calculateAllSpendCandidates(true)
    val necessaryInputCount = inputFundCandidates.view
      .scanLeft(Currency.Bitcoin.Zero)((accum, output) =>
      accum + Currency.Bitcoin.fromSatoshi(output.getValue))
      .takeWhile(_ < amount)
      .length
    inputFundCandidates.take(necessaryInputCount).toSet
  }

  private def addChangeOutput(tx: MutableTransaction, inputAmount: BitcoinAmount,
                              spentAmount: BitcoinAmount, changeAddress: Address): Unit = {
    val changeAmount = inputAmount - spentAmount
    require(!changeAmount.isNegative)
    if (changeAmount.isPositive) {
      tx.addOutput((inputAmount - spentAmount).asSatoshi, changeAddress)
    }
  }

  private def addMultisignOutput(tx: MutableTransaction, amount: BitcoinAmount,
                                 requiredSignatures: Seq[PublicKey]): Unit = {
    require(requiredSignatures.size > 1, "should have at least two signatures")
    tx.addOutput(
      amount.asSatoshi,
      ScriptBuilder.createMultiSigOutputScript(requiredSignatures.size, requiredSignatures)
    )
  }

  def createUnsignedTransaction(inputs: Seq[MutableTransactionOutput],
                                outputs: Seq[(PublicKey, BitcoinAmount)],
                                network: Network,
                                lockTime: Option[Long] = None): MutableTransaction = {
    val tx = new MutableTransaction(network)
    lockTime.foreach(tx.setLockTime)
    for (input <- inputs) { tx.addInput(input).setSequenceNumber(0) }
    for ((pubKey, amount) <- outputs) {
      tx.addOutput(amount.asSatoshi, pubKey)
    }
    tx
  }

  def signMultiSignedOutput(multiSignedDeposit: MutableTransaction, index: Int,
                            signAs: KeyPair, requiredSignatures: Seq[PublicKey]): TransactionSignature = {
    val script = ScriptBuilder.createMultiSigOutputScript(requiredSignatures.size, requiredSignatures)
    multiSignedDeposit.calculateSignature(index, signAs, script, SigHash.ALL, false)
  }

  def setMultipleSignatures(tx: MutableTransaction,
                            index: Int,
                            signatures: TransactionSignature*): Unit = {
    tx.getInput(index).setScriptSig(ScriptBuilder.createMultiSigInputScript(signatures))
  }

  def isValidSignature(transaction: MutableTransaction,
                       index: Int,
                       signature: TransactionSignature,
                       signerKey: KeyPair,
                       requiredSignatures: Seq[PublicKey]): Boolean = {
    val input = transaction.getInput(index)
    val script = ScriptBuilder.createMultiSigOutputScript(requiredSignatures.size, requiredSignatures)
    val hash = transaction.hashForSignature(index, script, SigHash.ALL, false)
    signerKey.verify(hash, signature)
  }

  def valueOf(outputs: Traversable[MutableTransactionOutput]): BitcoinAmount =
    outputs.map(funds => Currency.Bitcoin.fromSatoshi(funds.getValue)).reduce(_ + _)
}
