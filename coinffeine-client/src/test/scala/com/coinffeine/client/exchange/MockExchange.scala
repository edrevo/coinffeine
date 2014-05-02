package com.coinffeine.client.exchange

import scala.concurrent.Future
import scala.util.Try

import com.google.bitcoin.core.Transaction
import com.google.bitcoin.crypto.TransactionSignature
import org.joda.time.DateTime

import com.coinffeine.client.ExchangeInfo
import com.coinffeine.common.currency.Implicits._
import com.coinffeine.common.paymentprocessor.Payment

class MockExchange(override val exchangeInfo: ExchangeInfo) extends Exchange {
  private val offers = (1 to exchangeInfo.steps).map(idx => {
    val tx = new Transaction(exchangeInfo.network)
    tx.setLockTime(idx.toLong) // Ensures that generated transactions do not have the same hash
    tx
  })
  override def validateSignature(step: Int, signature: TransactionSignature): Try[Unit] = Try()
  override def getOffer(step: Int): Transaction = offers(step - 1)
  override def pay(step: Int): Future[Payment] = Future.successful(Payment(
    "paymentId", "sender", "receiver", 0.1 EUR, DateTime.now(), "description"))
  override def validatePayment(step: Int, paymentId: String): Try[Unit] = Try()
  override protected def sign(offer: Transaction): TransactionSignature = TransactionSignature.dummy()
  override def validateFinalSignature(signature: TransactionSignature): Try[Unit] = Try()
  override val finalOffer: Transaction = {
    val tx = new Transaction(exchangeInfo.network)
    tx.setLockTime(1500L)
    tx
  }
}
