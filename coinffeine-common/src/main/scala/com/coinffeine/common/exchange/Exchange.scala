package com.coinffeine.common.exchange

import java.security.SecureRandom
import scala.util.Random

import com.coinffeine.common._
import com.coinffeine.common.bitcoin._
import com.coinffeine.common.paymentprocessor.PaymentProcessor

/** All the necessary information to start an exchange between two peers. This is the point of view
  * of the parts before handshaking and also of the brokers.
  */
trait Exchange[+C <: FiatCurrency] {
  /** An identifier for the exchange */
  val id: Exchange.Id
  val amounts: Exchange.Amounts[C]
  /** Configurable parameters */
  val parameters: Exchange.Parameters
  /** PeerConnection of the buyer and the seller */
  val connections: Both[PeerConnection]
  val broker: Exchange.BrokerInfo
}

/** Relevant information for an ongoing exchange. This point fo view is only held by the parts
  * as contains information not make public to everyone on the network.
  */
trait OngoingExchange[+C <: FiatCurrency] extends Exchange[C] {
  /** Information about the parts */
  val participants: Both[Exchange.PeerInfo]

  def requiredSignatures: Both[PublicKey] = participants.map(_.bitcoinKey)
}

/** TODO: create different implementations of Exchange and OngoingExchange to limit what information
  * is available during the exchange by splitting this class.
  */
case class CompleteExchange[+C <: FiatCurrency] (
  override val id: Exchange.Id,
  override val amounts: Exchange.Amounts[C],
  override val parameters: Exchange.Parameters,
  override val connections: Both[PeerConnection],
  override val broker: Exchange.BrokerInfo,
  override val participants: Both[Exchange.PeerInfo]) extends OngoingExchange[C]

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
    * @param lockTime  The block number which will cause the refunds transactions to be valid
    * @param network   Bitcoin network
    */
  case class Parameters(lockTime: Long, network: Network)

  case class PeerInfo(paymentProcessorAccount: PaymentProcessor.AccountId, bitcoinKey: KeyPair)

  case class BrokerInfo(connection: PeerConnection)

  /** How the exchange is break down into steps */
  case class StepBreakdown(intermediateSteps: Int) {
    require(intermediateSteps > 0, s"Intermediate steps must be positive ($intermediateSteps given)")
    val totalSteps = intermediateSteps + 1
  }

  case class Amounts[+C <: FiatCurrency](bitcoinAmount: BitcoinAmount,
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

  case class Deposits(transactions: Both[ImmutableTransaction])
}
