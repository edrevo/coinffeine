package com.coinffeine.client.exchange

import java.math.BigInteger
import scala.util.{Failure, Success}

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.{ImmutableTransaction, MutableTransaction, TransactionSignature}
import com.coinffeine.common.exchange.{Exchange, MicroPaymentChannel}
import com.coinffeine.common.exchange.MicroPaymentChannel._

class MockMicroPaymentChannel(exchange: Exchange[_ <: FiatCurrency], step: Step = IntermediateStep(1))
  extends MicroPaymentChannel {

  override def currentStep: Step = step

  override def nextStep = {
    new MockMicroPaymentChannel(exchange, step match {
      case FinalStep => throw new IllegalArgumentException("Already at the end")
      case IntermediateStep(i) if i >= exchange.parameters.breakdown.intermediateSteps => FinalStep
      case IntermediateStep(i) => IntermediateStep(i + 1)
    })
  }

  override def validateCurrentTransactionSignatures(signatures: Signatures) =
    signatures match {
      case Signatures(MockMicroPaymentChannel.InvalidSignature, _) |
           Signatures(_, MockMicroPaymentChannel.InvalidSignature) =>
        Failure(new Error("Invalid signature"))
      case _ => Success {}
    }

  override def closingTransaction(counterpartSignatures: Signatures) = {
    val offerNumber = step match {
      case IntermediateStep(intermediateStep) => intermediateStep
      case FinalStep => exchange.parameters.breakdown.totalSteps
    }
    buildDummyTransaction(offerNumber - 1)
  }

  private def buildDummyTransaction(idx: Int) = {
    val tx = new MutableTransaction(exchange.parameters.network)
    tx.setLockTime(idx.toLong) // Ensures that generated transactions do not have the same hash
    ImmutableTransaction(tx)
  }

  override def signCurrentTransaction = MockMicroPaymentChannel.DummySignatures
}

object MockMicroPaymentChannel {
  /** Magic signature that is always rejected */
  val InvalidSignature = new TransactionSignature(BigInteger.valueOf(42), BigInteger.valueOf(42))
  val DummySignatures = Signatures(TransactionSignature.dummy, TransactionSignature.dummy)
}
