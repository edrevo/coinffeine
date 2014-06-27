package com.coinffeine.client.exchange

import scala.util.{Success, Try}

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.{ImmutableTransaction, MutableTransaction, TransactionSignature}
import com.coinffeine.common.exchange.Exchange
import com.coinffeine.common.exchange.MicroPaymentChannel._

class MockProtoMicroPaymentChannel(exchange: Exchange[_ <: FiatCurrency])
  extends ProtoMicroPaymentChannel {

  private val offers = (1 to exchange.parameters.breakdown.totalSteps).map(idx => {
    val tx = new MutableTransaction(exchange.parameters.network)
    tx.setLockTime(idx.toLong) // Ensures that generated transactions do not have the same hash
    ImmutableTransaction(tx)
  })

  override def validateStepTransactionSignatures(step: Step, signatures: Signatures): Try[Unit] =
    Success {}

  override def closingTransaction(step: Step, counterpartSignatures: Signatures) = {
    val offerNumber = step match {
      case IntermediateStep(intermediateStep) => intermediateStep
      case FinalStep => exchange.parameters.breakdown.totalSteps
    }
    offers(offerNumber - 1)
  }

  override def signStepTransaction(step: Step) =
    Signatures(TransactionSignature.dummy, TransactionSignature.dummy)
}
