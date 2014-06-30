package com.coinffeine.common.exchange

import java.security.SecureRandom
import scala.util.Random

import com.coinffeine.common._
import com.coinffeine.common.bitcoin._
import com.coinffeine.common.paymentprocessor.PaymentProcessor

/** A value class that contains all the necessary information relative to an exchange between
  * two peers
  *
  * @param id          An identifier for the exchange
  * @param parameters  Configurable parameters
  * @param buyer       Description of the buyer
  * @param seller      Description of the seller
  * @param broker      Connection parameters to one of the Coinffeine brokers
  */
case class Exchange[C <: FiatCurrency](
  id: Exchange.Id,
  parameters: Exchange.Parameters[C],
  buyer: Exchange.PeerInfo,
  seller: Exchange.PeerInfo,
  broker: Exchange.BrokerInfo) {

  val amounts: Exchange.Amounts[C] =
    Exchange.Amounts(parameters.bitcoinAmount, parameters.fiatAmount, parameters.breakdown)
}

object Exchange {

  case class Id(value: String) {
    override def toString = s"exchange:$value"
  }

  object Id {
    private val secureGenerator = new Random(new SecureRandom())

    def random() = Id(value = secureGenerator.nextString(12))
  }

  /** Configurable parameters of an exchange.
    *
    * @param bitcoinAmount The amount of bitcoins to exchange
    * @param fiatAmount The amount of fiat money to exchange
    * @param breakdown How the exchange is broken into steps
    * @param lockTime The block number which will cause the refunds transactions to be valid
    */
  case class Parameters[C <: FiatCurrency](bitcoinAmount: BitcoinAmount,
                                           fiatAmount: CurrencyAmount[C],
                                           breakdown: Exchange.StepBreakdown,
                                           lockTime: Long,
                                           network: Network) {
    require(bitcoinAmount.isPositive)
    require(fiatAmount.isPositive)
  }

  case class PeerInfo(connection: PeerConnection,
                      paymentProcessorAccount: PaymentProcessor.AccountId,
                      bitcoinKey: KeyPair)

  case class BrokerInfo(connection: PeerConnection)

  /** How the exchange is break down into steps */
  case class StepBreakdown(intermediateSteps: Int) {
    require(intermediateSteps > 0, s"Intermediate steps must be positive ($intermediateSteps given)")
    val totalSteps = intermediateSteps + 1
  }

  case class Amounts[C <: FiatCurrency](bitcoinAmount: BitcoinAmount,
                                        fiatAmount: CurrencyAmount[C],
                                        breakdown: Exchange.StepBreakdown) {
    require(bitcoinAmount.isPositive,
      s"bitcoin amount must be positive ($bitcoinAmount given)")
    require(fiatAmount.isPositive,
      s"fiat amount must be positive ($fiatAmount given)")

    /** Amount of bitcoins to exchange per intermediate step */
    val stepBitcoinAmount: BitcoinAmount = bitcoinAmount / breakdown.intermediateSteps
    /** Amount of fiat to exchange per intermediate step */
    val stepFiatAmount: CurrencyAmount[C] = fiatAmount / breakdown.intermediateSteps

    /** Total amount compromised in multisignature by the buyer */
    val buyerDeposit: BitcoinAmount = stepBitcoinAmount * 2
    /** Amount refundable by the buyer after a lock time */
    val buyerRefund: BitcoinAmount = buyerDeposit - stepBitcoinAmount

    /** Total amount compromised in multisignature by the seller */
    val sellerDeposit: BitcoinAmount = bitcoinAmount + stepBitcoinAmount
    /** Amount refundable by the seller after a lock time */
    val sellerRefund: BitcoinAmount = sellerDeposit - stepBitcoinAmount
  }

  trait Deposits {
    val transactions: Both[ImmutableTransaction]
  }
}
