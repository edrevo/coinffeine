package com.coinffeine.common.exchange

import java.math.BigInteger
import scala.util.{Failure, Success}

import com.coinffeine.common.bitcoin.{ImmutableTransaction, MutableTransaction, TransactionSignature}
import com.coinffeine.common.exchange.MicroPaymentChannel._

class MockMicroPaymentChannel private (exchange: AnyExchange, step: Step)
  extends MicroPaymentChannel {

  def this(exchange: AnyExchange) =
    this(exchange, IntermediateStep(1, exchange.parameters.breakdown))

  override val currentStep = step

  override def nextStep = new MockMicroPaymentChannel(exchange, step.next)

  override def validateCurrentTransactionSignatures(signatures: Signatures) =
    signatures match {
      case Signatures(MockMicroPaymentChannel.InvalidSignature, _) |
           Signatures(_, MockMicroPaymentChannel.InvalidSignature) =>
        Failure(new Error("Invalid signature"))
      case _ => Success {}
    }

  override def closingTransaction(counterpartSignatures: Signatures) =
    buildDummyTransaction(step.value - 1)

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
