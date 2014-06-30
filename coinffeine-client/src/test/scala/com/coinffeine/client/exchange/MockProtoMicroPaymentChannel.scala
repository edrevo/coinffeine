package com.coinffeine.client.exchange

import scala.util.{Failure, Success}

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.{ImmutableTransaction, MutableTransaction, TransactionSignature}
import com.coinffeine.common.exchange.MicroPaymentChannel._
import com.coinffeine.common.exchange.{Exchange, ProtoMicroPaymentChannel}

@deprecated
class MockProtoMicroPaymentChannel(exchange: Exchange[_ <: FiatCurrency])
  extends ProtoMicroPaymentChannel {

  import com.coinffeine.client.exchange.MockMicroPaymentChannel._

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

  override def closingTransaction(step: Step, counterpartSignatures: Signatures) =
    offers(step.value - 1)

  override def signStepTransaction(step: Step) =
    Signatures(TransactionSignature.dummy, TransactionSignature.dummy)
}
