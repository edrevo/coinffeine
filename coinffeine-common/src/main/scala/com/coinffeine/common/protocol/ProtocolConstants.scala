package com.coinffeine.common.protocol

import scala.concurrent.duration._

/** Protocol constants with default values.
  *
  * @constructor
  * @param commitmentConfirmations  Minimum number of confirmations to trust commitment TXs
  * @param resubmitRefundSignatureTimeout  Refund TX signature gets requested again after this
  *                                        timeout
  * @param refundSignatureAbortTimeout  Handshake is aborted after this time from handshake start
  * @param commitmentAbortTimeout  Maximum time than a broker will wait for buyer and seller
  *                                commitments
  */
case class ProtocolConstants(
  commitmentConfirmations: Int = 1,
  resubmitRefundSignatureTimeout: FiniteDuration = 10 seconds,
  refundSignatureAbortTimeout: FiniteDuration = 5 minutes,
  commitmentAbortTimeout: FiniteDuration = 5 minutes
)

object ProtocolConstants {

  val DefaultConstants = ProtocolConstants()

  trait Component {
    val protocolConstants: ProtocolConstants
  }
}
