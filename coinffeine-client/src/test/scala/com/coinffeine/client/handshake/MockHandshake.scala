package com.coinffeine.client.handshake

import java.math.BigInteger
import scala.util.{Failure, Random, Success}

import com.google.bitcoin.core.Wallet.SendRequest

import com.coinffeine.common.{BitcoinAmount, FiatCurrency}
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin._
import com.coinffeine.common.exchange.{Role, Exchange}

/** Create a mock handshake with random transactions.
  *
  * @param exchange       Info about the exchange being mocked
  * @param walletFactory  Creates wallets with a minimum balance on demand
  * @param network        Network in which transactions are generated
  */
class MockHandshake[C <: FiatCurrency](override val exchange: Exchange[C],
                                       override val role: Role,
                                       walletFactory: BitcoinAmount => Wallet,
                                       network: Network)  extends Handshake[C] {
  override val commitmentTransaction = randomImmutableTransaction()
  override val unsignedRefundTransaction = randomImmutableTransaction()
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

  private def randomImmutableTransaction() = ImmutableTransaction(randomTransaction())

  private def randomTransaction(): MutableTransaction = {
    val destination = new PublicKey()
    val amount = MutableTransaction.MinimumNonDustAmount * (Random.nextInt(10) + 10)
    val wallet = walletFactory(amount + 0.01.BTC)
    wallet.sendCoinsOffline(SendRequest.to(network, destination, amount.asSatoshi))
  }
}
