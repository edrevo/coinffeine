package com.coinffeine.client.handshake

import java.math.BigInteger
import scala.util.{Failure, Success}

import com.coinffeine.client.ExchangeInfo
import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.{MutableTransaction, TransactionSignature}
import com.coinffeine.common.protocol.MockTransaction

class MockHandshake[C <: FiatCurrency](
    override val exchangeInfo: ExchangeInfo[C]) extends Handshake[C] {
  override val commitmentTransaction = MockTransaction()
  override val refundTransaction = MockTransaction()
  val counterpartCommitmentTransaction = MockTransaction()
  val counterpartRefund = MockTransaction()
  val invalidRefundTransaction = MockTransaction()

  val refundSignature = new TransactionSignature(BigInteger.ZERO, BigInteger.ZERO)
  val counterpartRefundSignature = new TransactionSignature(BigInteger.ONE, BigInteger.ONE)

  override def signCounterpartRefundTransaction(txToSign: MutableTransaction) =
    if (txToSign == counterpartRefund) Success(counterpartRefundSignature)
    else Failure(new Error("Invalid refundSig"))

  override def validateRefundSignature(sig: TransactionSignature) =
    if (sig == refundSignature) Success(()) else Failure(new Error("Invalid signature!"))
}
