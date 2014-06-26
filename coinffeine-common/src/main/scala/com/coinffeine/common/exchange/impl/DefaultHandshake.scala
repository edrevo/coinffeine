package com.coinffeine.common.exchange.impl

import com.coinffeine.common.{BitcoinAmount, Currency, FiatCurrency}
import com.coinffeine.common.bitcoin.{ImmutableTransaction, MutableTransaction, TransactionSignature}
import com.coinffeine.common.exchange._
import com.coinffeine.common.exchange.Handshake.{InvalidRefundSignature, InvalidRefundTransaction}

private[impl] class DefaultHandshake[C <: FiatCurrency](
   role: Role,
   exchange: Exchange[C],
   override val myDeposit: ImmutableTransaction) extends Handshake[C] {

  override val myUnsignedRefund: ImmutableTransaction = UnsignedRefundTransaction(
    deposit = myDeposit,
    outputKey = role.me(exchange).bitcoinKey,
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
        myUnsignedRefund.get, index = 0, herSignature, role.her(exchange).bitcoinKey,
        Seq(exchange.buyer.bitcoinKey, exchange.seller.bitcoinKey))) {
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

  override def createMicroPaymentChannel(herDeposit: ImmutableTransaction) = {
    val buyerDeposit = role.buyer(myDeposit, herDeposit)
    val sellerDeposit = role.seller(myDeposit, herDeposit)
    new DefaultMicroPaymentChannel[C](role, exchange, Deposits(buyerDeposit, sellerDeposit))
  }

  private def signRefundTransaction(tx: MutableTransaction,
                                    expectedAmount: BitcoinAmount): TransactionSignature = {
    ensureValidRefundTransaction(ImmutableTransaction(tx), expectedAmount)
    TransactionProcessor.signMultiSignedOutput(
      multiSignedDeposit = tx,
      index = 0,
      signAs = role.me(exchange).bitcoinKey,
      requiredSignatures = Seq(exchange.buyer.bitcoinKey, exchange.seller.bitcoinKey)
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
