package com.coinffeine.client.handshake

import java.math.BigInteger

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin._
import com.coinffeine.common.exchange.{Exchange, Handshake, Role}
import com.coinffeine.common.exchange.Handshake.{InvalidRefundSignature, InvalidRefundTransaction}

/** Create a mock handshake with random transactions.
  *
  * @param exchange       Info about the exchange being mocked
  * @param role           Role being played
  */
class MockHandshake(exchange: Exchange[_ <: FiatCurrency], role: Role) extends Handshake {
  override val myDeposit = dummyImmutableTransaction(1)
  override val myUnsignedRefund = dummyImmutableTransaction(2)
  val mySignedRefund = dummyImmutableTransaction(3)
  val counterpartCommitmentTransaction = dummyTransaction(4)
  val counterpartRefund = dummyTransaction(5)
  val invalidRefundTransaction = dummyTransaction(6)

  val refundSignature = new TransactionSignature(BigInteger.ZERO, BigInteger.ZERO)
  val counterpartRefundSignature = new TransactionSignature(BigInteger.ONE, BigInteger.ONE)

  override def signHerRefund(txToSign: ImmutableTransaction) =
    if (txToSign.get == counterpartRefund) counterpartRefundSignature
    else throw new InvalidRefundTransaction(txToSign, "Invalid refundSig")

  override def signMyRefund(sig: TransactionSignature) =
    if (sig == refundSignature) mySignedRefund
    else throw new InvalidRefundSignature(myUnsignedRefund, sig)

  private def dummyImmutableTransaction(lockTime: Int) =
    ImmutableTransaction(dummyTransaction(lockTime))

  private def dummyTransaction(lockTime: Int): MutableTransaction = {
    val tx = new MutableTransaction(exchange.parameters.network)
    tx.setLockTime(lockTime)
    tx
  }
}
