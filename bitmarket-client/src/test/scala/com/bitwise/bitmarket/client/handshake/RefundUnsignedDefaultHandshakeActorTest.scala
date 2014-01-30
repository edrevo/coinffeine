package com.bitwise.bitmarket.client.handshake

import scala.concurrent.duration._
import scala.language.postfixOps

import com.bitwise.bitmarket.common.protocol.RejectExchange
import com.bitwise.bitmarket.common.protocol.gateway.MessageGateway.ForwardMessage

class RefundUnsignedDefaultHandshakeActorTest
  extends DefaultHandshakeActorTest("signature-timeout") {

  import HandshakeActor._

  override def protocolConstants = super.protocolConstants.copy(
    resubmitRefundSignatureTimeout = 10 seconds,
    refundSignatureAbortTimeout = 100 millis
  )

  "Handshakes without our refund signed" should "be aborted after a timeout" in {
    val result = listener.expectMsgClass(classOf[HandshakeResult]).refundSig
    result should be ('failure)
    listener.expectTerminated(actor)
  }

  it must "notify the broker that the exchange is rejected" in {
    gateway.fishForMessage() {
      case ForwardMessage(RejectExchange("id", _), handshake.`broker`) => true
      case _ => false
    }
  }
}
