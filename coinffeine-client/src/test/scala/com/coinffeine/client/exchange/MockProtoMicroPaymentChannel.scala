package com.coinffeine.client.exchange

import scala.concurrent.Future
import scala.util.{Success, Try}

import org.joda.time.DateTime

import com.coinffeine.client.ExchangeInfo
import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.{MutableTransaction, TransactionSignature}
import com.coinffeine.common.exchange.MicroPaymentChannel.StepSignatures
import com.coinffeine.common.paymentprocessor.Payment

class MockProtoMicroPaymentChannel[C <: FiatCurrency](exchangeInfo: ExchangeInfo[C]) extends ProtoMicroPaymentChannel[C] {

  private val offers = (1 to exchangeInfo.parameters.breakdown.intermediateSteps).map(idx => {
    val tx = new MutableTransaction(exchangeInfo.parameters.network)
    tx.setLockTime(idx.toLong) // Ensures that generated transactions do not have the same hash
    tx
  })
  override def validateSellersSignature(
      step: Int,
      signature0: TransactionSignature,
      signature1: TransactionSignature): Try[Unit] = Success {}
  override def getOffer(step: Int): MutableTransaction = offers(step - 1)
  override def pay(step: Int): Future[Payment[C]] = Future.successful(Payment(
    "paymentId", "sender", "receiver", exchangeInfo.fiatStepAmount, DateTime.now(), "description"))
  override def validatePayment(step: Int, paymentId: String): Future[Unit] = Future.successful {}
  override protected def sign(offer: MutableTransaction) =
    StepSignatures(TransactionSignature.dummy, TransactionSignature.dummy)
  override def validateSellersFinalSignature(
      signature0: TransactionSignature, signature1: TransactionSignature): Try[Unit] = Success {}
  override val finalOffer: MutableTransaction = {
    val tx = new MutableTransaction(exchangeInfo.parameters.network)
    tx.setLockTime(1500L)
    tx
  }

  override def getSignedOffer(step: Int, counterpartSignatures: StepSignatures) = getOffer(step)
}
