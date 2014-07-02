package com.coinffeine.common.exchange

import scala.util.{Failure, Success}

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.{ImmutableTransaction, MutableTransaction}
import com.coinffeine.common.exchange.MicroPaymentChannel._

class MockMicroPaymentChannel private (exchange: OngoingExchange[FiatCurrency], step: Step)
  extends MicroPaymentChannel {

  def this(exchange: OngoingExchange[FiatCurrency]) =
    this(exchange, IntermediateStep(1, exchange.amounts.breakdown))

  override val currentStep = step

  override def nextStep = new MockMicroPaymentChannel(exchange, step.next)

  override def validateCurrentTransactionSignatures(signatures: Signatures) =
    signatures match {
      case Signatures(MockExchangeProtocol.InvalidSignature, _) |
           Signatures(_, MockExchangeProtocol.InvalidSignature) =>
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

  override def signCurrentTransaction = MockExchangeProtocol.DummySignatures
}
