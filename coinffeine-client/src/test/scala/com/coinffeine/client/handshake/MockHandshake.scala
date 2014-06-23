package com.coinffeine.client.handshake

import java.math.BigInteger
import scala.util.{Failure, Random, Success}

import com.google.bitcoin.core.Wallet.SendRequest

import com.coinffeine.client.ExchangeInfo
import com.coinffeine.common.{BitcoinAmount, FiatCurrency}
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin._

/** Create a mock handshake with random transactions.
  *
  * @param exchangeInfo   Info about the exchange being mocked
  * @param walletFactory  Creates wallets with a minimum balance on demand
  * @param network        Network in which transactions are generated
  */
class MockHandshake[C <: FiatCurrency](override val exchangeInfo: ExchangeInfo[C],
                                       walletFactory: BitcoinAmount => Wallet,
                                       network: Network)  extends Handshake[C] {
  override val commitmentTransaction = randomTransaction()
  override val refundTransaction = randomTransaction()
  val counterpartCommitmentTransaction = randomTransaction()
  val counterpartRefund = randomTransaction()
  val invalidRefundTransaction = randomTransaction()

  val refundSignature = new TransactionSignature(BigInteger.ZERO, BigInteger.ZERO)
  val counterpartRefundSignature = new TransactionSignature(BigInteger.ONE, BigInteger.ONE)

  override def signCounterpartRefundTransaction(txToSign: MutableTransaction) =
    if (txToSign == counterpartRefund) Success(counterpartRefundSignature)
    else Failure(new Error("Invalid refundSig"))

  override def validateRefundSignature(sig: TransactionSignature) =
    if (sig == refundSignature) Success(()) else Failure(new Error("Invalid signature!"))

  private def randomTransaction(): MutableTransaction = {
    val destination = new PublicKey()
    val amount = MutableTransaction.MinimumNonDustAmount * (Random.nextInt(10) + 10)
    val wallet = walletFactory(amount + 0.01.BTC)
    wallet.sendCoinsOffline(SendRequest.to(network, destination, amount.asSatoshi))
  }
}
