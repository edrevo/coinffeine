package com.coinffeine.client.exchange

import java.math.BigInteger
import scala.util.{Failure, Success, Try}

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.{ImmutableTransaction, MutableTransaction, TransactionSignature}
import com.coinffeine.common.exchange.{ProtoMicroPaymentChannel, Exchange}
import com.coinffeine.common.exchange.MicroPaymentChannel._

class MockProtoMicroPaymentChannel(exchange: Exchange[_ <: FiatCurrency])
  extends ProtoMicroPaymentChannel {

  import MockProtoMicroPaymentChannel._

  private val offers = (1 to exchange.parameters.breakdown.totalSteps).map(idx => {
    val tx = new MutableTransaction(exchange.parameters.network)
    tx.setLockTime(idx.toLong) // Ensures that generated transactions do not have the same hash
    ImmutableTransaction(tx)
  })

  override def validateStepTransactionSignatures(step: Step, signatures: Signatures) =
    signatures match {
      case Signatures(InvalidSignature, _) | Signatures(_, InvalidSignature) =>
        Failure(new Error("Invalid signature"))
      case _ => Success {}
    }

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

object MockProtoMicroPaymentChannel {
  /** Magic signature that is always rejected */
  val InvalidSignature = new TransactionSignature(BigInteger.valueOf(42), BigInteger.valueOf(42))
}
