package com.coinffeine.client.exchange

import scala.util.{Success, Try}

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.{MutableTransaction, TransactionSignature}
import com.coinffeine.common.exchange.Exchange
import com.coinffeine.common.exchange.MicroPaymentChannel.StepSignatures

class MockProtoMicroPaymentChannel[C <: FiatCurrency](exchange: Exchange[C])
  extends ProtoMicroPaymentChannel[C] {

  private val offers = (1 to exchange.parameters.breakdown.intermediateSteps).map(idx => {
    val tx = new MutableTransaction(exchange.parameters.network)
    tx.setLockTime(idx.toLong) // Ensures that generated transactions do not have the same hash
    tx
  })
  override def validateSellersSignature(
      step: Int,
      signature0: TransactionSignature,
      signature1: TransactionSignature): Try[Unit] = Success {}
  override def getOffer(step: Int): MutableTransaction = offers(step - 1)
  override protected def sign(offer: MutableTransaction) =
    StepSignatures(TransactionSignature.dummy, TransactionSignature.dummy)
  override def validateSellersFinalSignature(
      signature0: TransactionSignature, signature1: TransactionSignature): Try[Unit] = Success {}
  override val finalOffer: MutableTransaction = {
    val tx = new MutableTransaction(exchange.parameters.network)
    tx.setLockTime(1500L)
    tx
  }

  override def getSignedOffer(step: Int, counterpartSignatures: StepSignatures) = getOffer(step)
}
