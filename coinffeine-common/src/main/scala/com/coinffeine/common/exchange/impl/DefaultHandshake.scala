package com.coinffeine.common.exchange.impl

import com.google.bitcoin.core.Transaction
import com.google.bitcoin.core.TransactionConfidence.ConfidenceType
import com.google.bitcoin.crypto.TransactionSignature

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.exchange._

case class DefaultHandshake[C <: FiatCurrency](
   role: Role,
   override val exchange: DefaultExchange[C],
   override val myDeposit: ImmutableTransaction) extends Handshake[C](exchange) {

  override val myUnsignedRefund: ImmutableTransaction = UnsignedRefundTransaction(
    deposit = myDeposit,
    outputKey = role.me(exchange).bitcoinKey,
    outputAmount = role.myRefundAmount(exchange.amounts),
    lockTime = exchange.parameters.lockTime,
    network = exchange.parameters.network
  )

  @throws[InvalidRefundTransaction]
  override def signHerRefund(herRefund: ImmutableTransaction): TransactionSignature = {
    val tx = herRefund.get
    ensureValidRefundTransaction(tx)
    signRefundTransaction(tx)
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
      ensureValidRefundTransaction(tx)
      val mySignature = signRefundTransaction(tx)
      val buyerSignature = role.buyer(mySignature, herSignature)
      val sellerSignature = role.seller(mySignature, herSignature)
      TransactionProcessor.setMultipleSignatures(tx, 0, buyerSignature, sellerSignature)
      tx
    }
  }

  private def signRefundTransaction(tx: Transaction): TransactionSignature = {
    TransactionProcessor.signMultiSignedOutput(
      multiSignedDeposit = tx,
      index = 0,
      signAs = role.me(exchange).bitcoinKey,
      requiredSignatures = Seq(exchange.buyer.bitcoinKey, exchange.seller.bitcoinKey)
    )
  }

  override def createMicroPaymentChannel(herDeposit: ImmutableTransaction) = {
    val buyerDeposit = role.buyer(myDeposit, herDeposit)
    val sellerDeposit = role.seller(myDeposit, herDeposit)
    DefaultMicroPaymentChannel[C](role, exchange, Deposits(buyerDeposit, sellerDeposit))
  }

  private def ensureValidRefundTransaction(refundTx: Transaction) = {
    // TODO: Is this enough to ensure we can sign?
    require(refundTx.isTimeLocked)
    require(refundTx.getLockTime == exchange.parameters.lockTime)
    require(refundTx.getInputs.size == 1)
    require(refundTx.getConfidence.getConfidenceType == ConfidenceType.UNKNOWN)
  }
}
