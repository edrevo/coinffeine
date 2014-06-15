package com.coinffeine.client.exchange

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

import com.google.bitcoin.core.Transaction
import com.google.bitcoin.crypto.TransactionSignature
import org.joda.time.DateTime

import com.coinffeine.client.ExchangeInfo
import com.coinffeine.common.Currency
import com.coinffeine.common.paymentprocessor.Payment

class MockExchange(override val exchangeInfo: ExchangeInfo[Currency.Euro.type]) extends Exchange[Currency.Euro.type] {
  this: UserRole =>

  private val offers = (1 to exchangeInfo.steps).map(idx => {
    val tx = new Transaction(exchangeInfo.network)
    tx.setLockTime(idx.toLong) // Ensures that generated transactions do not have the same hash
    tx
  })
  override def validateSellersSignature(
      step: Int,
      signature0: TransactionSignature,
      signature1: TransactionSignature): Try[Unit] = Try()
  override def getOffer(step: Int): Transaction = offers(step - 1)
  override def pay(step: Int): Future[Payment[Currency.Euro.type]] = Future.successful(Payment(
    "paymentId", "sender", "receiver", Currency.Euro(0.1), DateTime.now(), "description"))
  override def validatePayment(step: Int, paymentId: String): Future[Unit] = Future()
  override protected def sign(offer: Transaction): (TransactionSignature, TransactionSignature) =
    (TransactionSignature.dummy, TransactionSignature.dummy)
  override def validateSellersFinalSignature(
      signature0: TransactionSignature, signature1: TransactionSignature): Try[Unit] = Try()
  override val finalOffer: Transaction = {
    val tx = new Transaction(exchangeInfo.network)
    tx.setLockTime(1500L)
    tx
  }

  override def getSignedOffer(
      step: Int,
      counterpartSignatures: (TransactionSignature, TransactionSignature)): Transaction =
    getOffer(step)
}
