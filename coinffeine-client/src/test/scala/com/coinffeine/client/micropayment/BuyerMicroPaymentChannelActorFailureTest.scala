package com.coinffeine.client.micropayment

import scala.concurrent.duration._

import akka.actor.Props
import akka.testkit.TestProbe

import com.coinffeine.client.CoinffeineClientTest
import com.coinffeine.client.CoinffeineClientTest.BuyerPerspective
import com.coinffeine.client.exchange.MockMicroPaymentChannel
import com.coinffeine.client.handshake.MockExchangeProtocol
import com.coinffeine.client.micropayment.MicroPaymentChannelActor._
import com.coinffeine.common.bitcoin.TransactionSignature
import com.coinffeine.common.exchange.BuyerRole
import com.coinffeine.common.exchange.MicroPaymentChannel.Signatures
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.messages.exchange.StepSignatures

class BuyerMicroPaymentChannelActorFailureTest
  extends CoinffeineClientTest("buyerExchange") with BuyerPerspective {

  val protocolConstants = ProtocolConstants(exchangeSignatureTimeout = 0.5 seconds)
  val exchangeProtocol = new MockExchangeProtocol
  val signatures = Signatures(TransactionSignature.dummy, TransactionSignature.dummy)

  trait Fixture {
    val listener = TestProbe()
    val paymentProcessor = TestProbe()
    val actor = system.actorOf(Props(new BuyerMicroPaymentChannelActor(new MockExchangeProtocol)))
    listener.watch(actor)

    def startMicroPaymentChannel(): Unit = {
      actor ! StartMicroPaymentChannel(exchange, BuyerRole, MockExchangeProtocol.DummyDeposits,
        protocolConstants, paymentProcessor.ref, gateway.ref, Set(listener.ref))
    }
  }

  "The buyer exchange actor" should "return a failure message if the seller does not provide the" +
    " step signature within the specified timeout" in new Fixture {
    startMicroPaymentChannel()
    val failure = listener.expectMsgClass(classOf[ExchangeFailure])
    failure.lastOffer should be (None)
    failure.cause shouldBe a [TimeoutException]
  }

  it should "return the last signed offer when a timeout happens" in new Fixture{
    startMicroPaymentChannel()
    actor ! fromCounterpart(StepSignatures(exchange.id, 1, signatures))
    val failure = listener.expectMsgClass(classOf[ExchangeFailure])
    failure.lastOffer should be ('defined)
    failure.cause shouldBe a [TimeoutException]
  }

  it should "return a failure message if the seller provides an invalid signature" in new Fixture {
    startMicroPaymentChannel()
    val invalidDeposits = signatures.copy(
      buyer = MockMicroPaymentChannel.InvalidSignature
    )
    actor ! fromCounterpart(StepSignatures(exchange.id, 1, invalidDeposits))
    val failure = listener.expectMsgClass(classOf[ExchangeFailure])
    failure.lastOffer should be ('empty)
    failure.cause shouldBe an [InvalidStepSignatures]
    failure.cause.asInstanceOf[InvalidStepSignatures].step should be (1)
  }
}
