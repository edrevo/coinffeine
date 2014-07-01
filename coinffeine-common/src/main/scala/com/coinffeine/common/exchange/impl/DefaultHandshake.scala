package com.coinffeine.common.exchange.impl

import com.coinffeine.common.{BitcoinAmount, Currency}
import com.coinffeine.common.bitcoin.{ImmutableTransaction, MutableTransaction, TransactionSignature}
import com.coinffeine.common.exchange._
import com.coinffeine.common.exchange.Handshake.{InvalidRefundSignature, InvalidRefundTransaction}

private[impl] class DefaultHandshake(
   exchange: AnyExchange,
   role: Role,
   override val myDeposit: ImmutableTransaction) extends Handshake {

  override val myUnsignedRefund: ImmutableTransaction = UnsignedRefundTransaction(
    deposit = myDeposit,
    outputKey = exchange.participants(role).bitcoinKey,
    outputAmount = role.myRefundAmount(exchange.amounts),
    lockTime = exchange.parameters.lockTime,
    network = exchange.parameters.network
  )

  @throws[InvalidRefundTransaction]
  override def signHerRefund(herRefund: ImmutableTransaction): TransactionSignature = {
    signRefundTransaction(herRefund.get, expectedAmount = role.herRefundAmount(exchange.amounts))
  }

  @throws[InvalidRefundSignature]
  override def signMyRefund(herSignature: TransactionSignature) = {
    if (!TransactionProcessor.isValidSignature(
        myUnsignedRefund.get, index = 0, herSignature,
        signerKey = exchange.participants(role.counterpart).bitcoinKey,
        exchange.requiredSignatures)) {
      throw InvalidRefundSignature(myUnsignedRefund, herSignature)
    }
    ImmutableTransaction {
      val tx = myUnsignedRefund.get
      val mySignature = signRefundTransaction(
        tx,
        expectedAmount = role.myRefundAmount(exchange.amounts))
      val buyerSignature = role.buyer(mySignature, herSignature)
      val sellerSignature = role.seller(mySignature, herSignature)
      TransactionProcessor.setMultipleSignatures(tx, 0, buyerSignature, sellerSignature)
      tx
    }
  }

  private def signRefundTransaction(tx: MutableTransaction,
                                    expectedAmount: BitcoinAmount): TransactionSignature = {
    ensureValidRefundTransaction(ImmutableTransaction(tx), expectedAmount)
    TransactionProcessor.signMultiSignedOutput(
      multiSignedDeposit = tx,
      index = 0,
      signAs = exchange.participants(role).bitcoinKey,
      exchange.requiredSignatures
    )
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
}
