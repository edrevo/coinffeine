package com.coinffeine.common.protocol

import scala.concurrent.duration._
import scala.language.postfixOps

/** Protocol constants with default values.
  *
  * @constructor
  * @param commitmentConfirmations  Minimum number of confirmations to trust commitment TXs
  * @param resubmitRefundSignatureTimeout  Refund TX signature gets requested again after this
  *                                        timeout
  * @param refundSignatureAbortTimeout  Handshake is aborted after this time from handshake start
  * @param commitmentAbortTimeout  Maximum time than a broker will wait for buyer and seller
  *                                commitments
  * @param exchangeSignatureTimeout Amount of time the actor will wait for a step signature
  * @param exchangePaymentProofTimeout Amount of time the actor will wait for a payment proof
  * @param orderExpirationInterval Time that orders take to be discarded if not renewed
  * @param orderResubmitInterval   Open orders should be resubmitted after this interval to avoid
  *                                being discarded
  * @param refundSafetyBlockCount The number of blocks before the refund can be broadcast where we
  *                               want to finish the exchange forcefully.
  * @param version                 Protocol version
  */
case class ProtocolConstants(
  commitmentConfirmations: Int = 1,
  resubmitRefundSignatureTimeout: FiniteDuration = 10 seconds,
  refundSignatureAbortTimeout: FiniteDuration = 5 minutes,
  commitmentAbortTimeout: FiniteDuration = 5 minutes,
  exchangeSignatureTimeout: FiniteDuration = 5 minutes,
  exchangePaymentProofTimeout: FiniteDuration = 5 minutes,
  orderExpirationInterval: FiniteDuration = 1 minute,
  orderResubmitInterval: FiniteDuration = 30 seconds,
  refundSafetyBlockCount: Int = 2,
  version: Version = Version(major = 0, minor = 1)
)

object ProtocolConstants {

  val DefaultConstants = ProtocolConstants()

  trait Component {
    val protocolConstants: ProtocolConstants
  }

  trait DefaultComponent extends Component {
    override val protocolConstants = DefaultConstants
  }
}
