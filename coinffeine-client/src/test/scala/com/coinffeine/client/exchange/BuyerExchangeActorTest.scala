package com.coinffeine.client.exchange

import scala.concurrent.duration._

import akka.actor.Props
import akka.testkit.TestProbe
import com.google.bitcoin.crypto.TransactionSignature
import org.scalatest.mock.MockitoSugar

import com.coinffeine.client.CoinffeineClientTest
import com.coinffeine.client.exchange.ExchangeActor.{ExchangeSuccess, StartExchange}
import com.coinffeine.common.PeerConnection
import com.coinffeine.common.currency.CurrencyCode
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.{ReceiveMessage, Subscribe}
import com.coinffeine.common.protocol.messages.brokerage.CancelOrder
import com.coinffeine.common.protocol.messages.exchange.{StepSignature, PaymentProof}

class BuyerExchangeActorTest extends CoinffeineClientTest("buyerExchange") with MockitoSugar {
  val listener = TestProbe()
  val exchangeInfo = sampleExchangeInfo
  val protocolConstants = ProtocolConstants(
    commitmentConfirmations = 1,
    resubmitRefundSignatureTimeout = 1 second,
    refundSignatureAbortTimeout = 1 minute)
  val exchange = new MockExchange(exchangeInfo)
  override val broker: PeerConnection = exchangeInfo.broker
  override val counterpart: PeerConnection = exchangeInfo.counterpart
  val actor = system.actorOf(
    Props(new BuyerExchangeActor(exchange, protocolConstants)),
    "buyer-exchange-actor"
  )
  listener.watch(actor)

  "The buyer exchange actor" should "subscribe to the relevant messages when initialized" in {
    gateway.expectNoMsg()
    actor ! StartExchange(gateway.ref, Set(listener.ref))
    val Subscribe(filter) = gateway.expectMsgClass(classOf[Subscribe])
    val relevantOfferAccepted = StepSignature("id", TransactionSignature.dummy())
    val irrelevantOfferAccepted = StepSignature("another-id", TransactionSignature.dummy())
    val anotherPeer = PeerConnection("some-random-peer")
    filter(fromCounterpart(relevantOfferAccepted)) should be (true)
    filter(ReceiveMessage(relevantOfferAccepted, anotherPeer)) should be (false)
    filter(fromCounterpart(irrelevantOfferAccepted)) should be (false)
    val randomMessage = CancelOrder(CurrencyCode.EUR.currency)
    filter(ReceiveMessage(randomMessage, exchangeInfo.counterpart)) should be (false)
  }

  it should "respond to step signature messages by sending a payment until all " +
    "steps have are done" in {
      for (i <- 1 to exchangeInfo.steps) {
        actor ! fromCounterpart(StepSignature(exchangeInfo.id, TransactionSignature.dummy))
        val paymentMsg = PaymentProof(exchangeInfo.id, "paymentId")
        shouldForward(paymentMsg) to counterpart
        gateway.expectNoMsg(100 milliseconds)
      }
    }

  it should "send a notification to the listeners once the exchange has finished" in {
    actor ! fromCounterpart(StepSignature(exchangeInfo.id, TransactionSignature.dummy))
    listener.expectMsg(ExchangeSuccess)
  }
}
