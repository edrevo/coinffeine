package com.coinffeine.common.exchange.impl

import scala.collection.JavaConversions._

import com.google.bitcoin.core._
import com.google.bitcoin.core.Transaction.SigHash
import com.google.bitcoin.crypto.TransactionSignature
import com.google.bitcoin.script.ScriptBuilder

import com.coinffeine.common.{BitcoinAmount, Currency}
import com.coinffeine.common.Currency.Implicits._

/** This trait encapsulates the transaction processing actions. */
object TransactionProcessor {

  def createMultisignDeposit(userWallet: Wallet,
                             amountToCommit: BitcoinAmount,
                             requiredSignatures: Seq[ECKey],
                             network: NetworkParameters): Transaction = {
    require(amountToCommit.isPositive, "Amount to commit must be greater than zero")

    val inputFunds = collectFunds(userWallet, amountToCommit)
    val totalInputFunds =
      inputFunds.map(funds => Currency.Bitcoin.fromSatoshi(funds.getValue)).reduce(_ + _)
    require(totalInputFunds >= amountToCommit,
      "Input funds must cover the amount of funds to commit")

    val tx = new Transaction(network)
    inputFunds.foreach(tx.addInput)
    addMultisignOutput(tx, amountToCommit, requiredSignatures)
    addChangeOutput(tx, totalInputFunds, amountToCommit, userWallet.getChangeAddress)
    tx.signInputs(SigHash.ALL, userWallet)
    tx
  }

  private def collectFunds(userWallet: Wallet, amount: BitcoinAmount): Set[TransactionOutput] = {
    val inputFundCandidates = userWallet.calculateAllSpendCandidates(true)
    val necessaryInputCount = inputFundCandidates.view
      .scanLeft(Currency.Bitcoin.Zero)((accum, output) =>
      accum + Currency.Bitcoin.fromSatoshi(output.getValue))
      .takeWhile(_ < amount)
      .length
    inputFundCandidates.take(necessaryInputCount).toSet
  }

  private def addChangeOutput(tx: Transaction, inputAmount: BitcoinAmount,
                              spentAmount: BitcoinAmount, changeAddress: Address): Unit = {
    val changeAmount = inputAmount - spentAmount
    require(!changeAmount.isNegative)
    if (changeAmount.isPositive) {
      tx.addOutput((inputAmount - spentAmount).asSatoshi, changeAddress)
    }
  }

  private def addMultisignOutput(tx: Transaction, amount: BitcoinAmount,
                                 requiredSignatures: Seq[ECKey]): Unit = {
    require(requiredSignatures.size > 1, "should have at least two signatures")
    tx.addOutput(
      amount.asSatoshi,
      ScriptBuilder.createMultiSigOutputScript(requiredSignatures.size, requiredSignatures)
    )
  }

  def createUnsignedTransaction(inputs: Seq[TransactionOutput],
                                outputs: Seq[(ECKey, BitcoinAmount)],
                                network: NetworkParameters,
                                lockTime: Option[Long] = None): Transaction = {
    val tx = new Transaction(network)
    lockTime.foreach(tx.setLockTime)
    for (input <- inputs) { tx.addInput(input).setSequenceNumber(0) }
    for ((pubKey, amount) <- outputs) {
      tx.addOutput(amount.asSatoshi, pubKey)
    }
    tx
  }

  def addSignatures(tx: Transaction, signatures: (Int, TransactionSignature)*): Transaction = ???

  def multisign(tx: Transaction, inputIndex: Int, keys: ECKey*): TransactionSignature = ???

  def sign(tx: Transaction, inputIndex: Int, key: ECKey): TransactionSignature = ???

  def isValidSignature(transaction: Transaction,
                       outputIndex: Int,
                       signature: TransactionSignature): Boolean = ???

  def areValidSignatures(transaction: Transaction,
                         signatures: Seq[TransactionSignature]): Boolean =
    signatures.zipWithIndex.forall {
      case (sign: TransactionSignature, index) => isValidSignature(transaction, index, sign)
    }
}
