package com.bitwise.bitmarket.client

import scala.concurrent.duration.FiniteDuration

/** Protocol constants.
  *
  * @constructor
  * @param commitmentConfirmations  Minimum number of confirmations to trust commitment TXs
  * @param resubmitRefundSignatureTimeout  Refund TX signature gets requested again after this
  *                                        timeout
  * @param refundSignatureAbortTimeout  Handshake is aborted after this time from handshake start
  */
case class ProtocolConstants(
  commitmentConfirmations: Int,
  resubmitRefundSignatureTimeout: FiniteDuration,
  refundSignatureAbortTimeout: FiniteDuration
)

object ProtocolConstants {
  trait Component {
    val protocolConstants: ProtocolConstants
  }
}
