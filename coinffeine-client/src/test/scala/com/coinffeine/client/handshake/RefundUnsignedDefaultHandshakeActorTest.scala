package com.coinffeine.client.handshake

import scala.concurrent.duration._

import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.ForwardMessage
import com.coinffeine.common.protocol.messages.handshake.ExchangeRejection

class RefundUnsignedDefaultHandshakeActorTest
  extends DefaultHandshakeActorTest("signature-timeout") {

  import HandshakeActor._

  override def protocolConstants = ProtocolConstants(
    commitmentConfirmations = 1,
    resubmitRefundSignatureTimeout = 10 seconds,
    refundSignatureAbortTimeout = 100 millis
  )

  "Handshakes without our refund signed" should "be aborted after a timeout" in {
    givenActorIsInitialized()
    listener.expectMsgClass(classOf[HandshakeFailure])
    listener.expectTerminated(actor)
  }

  it must "notify the broker that the exchange is rejected" in {
    val broker = handshake.exchangeInfo.broker.connection
    gateway.fishForMessage() {
      case ForwardMessage(ExchangeRejection(`exchangeId`, _), `broker`) => true
      case _ => false
    }
  }
}
