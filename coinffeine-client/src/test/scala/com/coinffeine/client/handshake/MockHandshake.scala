package com.coinffeine.client.handshake

import java.math.BigInteger
import scala.util.Random

import com.google.bitcoin.core.Wallet.SendRequest

import com.coinffeine.common.{BitcoinAmount, FiatCurrency}
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin._
import com.coinffeine.common.exchange.{Exchange, Role}
import com.coinffeine.common.exchange.Handshake.{InvalidRefundSignature, InvalidRefundTransaction}

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
  override val myDeposit = randomImmutableTransaction()
  override val myUnsignedRefund = randomImmutableTransaction()
  val mySignedRefund = randomImmutableTransaction()
  val counterpartCommitmentTransaction = randomTransaction()
  val counterpartRefund = randomTransaction()
  val invalidRefundTransaction = randomTransaction()

  val refundSignature = new TransactionSignature(BigInteger.ZERO, BigInteger.ZERO)
  val counterpartRefundSignature = new TransactionSignature(BigInteger.ONE, BigInteger.ONE)

  override def signHerRefund(txToSign: ImmutableTransaction) =
    if (txToSign.get == counterpartRefund) counterpartRefundSignature
    else throw new InvalidRefundTransaction(txToSign, "Invalid refundSig")

  override def signMyRefund(sig: TransactionSignature) =
    if (sig == refundSignature) mySignedRefund
    else throw new InvalidRefundSignature(myUnsignedRefund, sig)

  private def randomImmutableTransaction() = ImmutableTransaction(randomTransaction())

  private def randomTransaction(): MutableTransaction = {
    val destination = new PublicKey()
    val amount = MutableTransaction.MinimumNonDustAmount * (Random.nextInt(10) + 10)
    val wallet = walletFactory(amount + 0.01.BTC)
    wallet.sendCoinsOffline(SendRequest.to(network, destination, amount.asSatoshi))
  }
}
