package com.coinffeine.client.exchange

import scala.util.{Success, Try}

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.{MutableTransaction, TransactionSignature}
import com.coinffeine.common.exchange.Exchange
import com.coinffeine.common.exchange.MicroPaymentChannel._

class MockProtoMicroPaymentChannel[C <: FiatCurrency](exchange: Exchange[C])
  extends ProtoMicroPaymentChannel[C] {

  private val offers = (1 to exchange.parameters.breakdown.intermediateSteps).map(idx => {
    val tx = new MutableTransaction(exchange.parameters.network)
    tx.setLockTime(idx.toLong) // Ensures that generated transactions do not have the same hash
    tx
  })

  override def validateSellersSignature(step: Step, signatures: Signatures): Try[Unit] = Success {}

  override def getOffer(step: Step): MutableTransaction = step match {
    case IntermediateStep(i) => offers(i - 1)
    case FinalStep =>
      val tx = new MutableTransaction(exchange.parameters.network)
      tx.setLockTime(1500L)
      tx
  }

  override protected def sign(offer: MutableTransaction) =
    Signatures(TransactionSignature.dummy, TransactionSignature.dummy)

  override def getSignedOffer(step: IntermediateStep, counterpartSignatures: Signatures) =
    getOffer(step)
}
