package com.coinffeine.common.exchange

import scala.concurrent.duration.FiniteDuration

import com.coinffeine.common._
import com.coinffeine.common.bitcoin._
import com.coinffeine.common.paymentprocessor.PaymentProcessor
import com.coinffeine.common.protocol.messages.exchange.ExchangeId

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
  id: ExchangeId,
  parameters: Exchange.Parameters[C],
  buyer: Exchange.PeerInfo[KeyPair],
  seller: Exchange.PeerInfo[KeyPair],
  broker: Exchange.BrokerInfo) {

  val amounts: Exchange.Amounts[C] =
    Exchange.Amounts(parameters.bitcoinAmount, parameters.fiatAmount, parameters.breakdown)
}

object Exchange {

  /** Configurable parameters of an exchange.
    *
    * @param bitcoinAmount The amount of bitcoins to exchange
    * @param fiatAmount The amount of fiat money to exchange
    * @param breakdown How the exchange is broken into steps
    * @param lockTime The block number which will cause the refunds transactions to be valid
    * @param commitmentConfirmations  Minimum number of confirmations to consider the deposits
    *                                 reliable enough
    * @param resubmitRefundSignatureTimeout  Time to wait before asking again for a refund signature
    * @param refundSignatureAbortTimeout  Time to get the refund transaction signature before
    *                                     aborting the exchange
    */
  case class Parameters[C <: FiatCurrency](bitcoinAmount: BitcoinAmount,
                                           fiatAmount: CurrencyAmount[C],
                                           breakdown: Exchange.StepBreakdown,
                                           lockTime: Long,
                                           commitmentConfirmations: Int,
                                           resubmitRefundSignatureTimeout: FiniteDuration,
                                           refundSignatureAbortTimeout: FiniteDuration,
                                           network: Network)

  case class PeerInfo[KeyPair](connection: PeerConnection,
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
}
