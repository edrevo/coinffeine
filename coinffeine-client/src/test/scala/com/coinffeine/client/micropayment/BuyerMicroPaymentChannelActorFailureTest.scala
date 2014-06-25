package com.coinffeine.client.micropayment

import scala.concurrent.duration._
import scala.util.{Failure, Try}

import akka.actor.Props
import akka.testkit.TestProbe

import com.coinffeine.client.CoinffeineClientTest
import com.coinffeine.client.exchange.{BuyerUser, MockExchange}
import com.coinffeine.client.micropayment.MicroPaymentChannelActor._
import com.coinffeine.common.PeerConnection
import com.coinffeine.common.Currency.Euro
import com.coinffeine.common.exchange.MicroPaymentChannel.{StepSignatures => Signatures}
import com.coinffeine.common.bitcoin.TransactionSignature
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.messages.exchange.StepSignatures

class BuyerMicroPaymentChannelActorFailureTest extends CoinffeineClientTest("buyerExchange") {

  val exchangeInfo = sampleExchangeInfo
  override val broker: PeerConnection = exchangeInfo.broker.connection
  override val counterpart: PeerConnection = exchangeInfo.counterpart.connection
  val protocolConstants = ProtocolConstants(exchangeSignatureTimeout = 0.5 seconds)
  val exchange = new MockExchange(exchangeInfo) with BuyerUser[Euro.type]
  val dummySig = TransactionSignature.dummy

  trait Fixture {
    val listener = TestProbe()
    val actor = system.actorOf(Props[BuyerMicroPaymentChannelActor[Euro.type]])
    listener.watch(actor)
  }

  "The buyer exchange actor" should "return a failure message if the seller does not provide the" +
    " step signature within the specified timeout" in new Fixture{
      actor ! StartMicroPaymentChannel(exchange, protocolConstants, gateway.ref, Set(listener.ref))
      val failure = listener.expectMsgClass(classOf[ExchangeFailure])
      failure.lastOffer should be (None)
      failure.cause.isInstanceOf[TimeoutException] should be (true)
  }

  it should "return the last signed offer when a timeout happens" in new Fixture{
    actor ! StartMicroPaymentChannel(exchange, protocolConstants, gateway.ref, Set(listener.ref))
    actor ! fromCounterpart(StepSignatures(exchangeInfo.id, 1, dummySig, dummySig))
    val failure = listener.expectMsgClass(classOf[ExchangeFailure])
    failure.lastOffer should be (Some(exchange.getSignedOffer(1, Signatures(dummySig, dummySig))))
    failure.cause.isInstanceOf[TimeoutException] should be (true)
  }

  it should "return a failure message if the seller provides an invalid signature" in new Fixture {
    val error = new Error("Some error")
    val rejectingExchange = new MockExchange(exchangeInfo) with BuyerUser[Euro.type] {
      override def validateSellersSignature(
          step: Int,
          signature0: TransactionSignature,
          signature1: TransactionSignature): Try[Unit] = Failure(error)
    }
    actor ! StartMicroPaymentChannel(rejectingExchange, protocolConstants, gateway.ref, Set(listener.ref))
    actor ! fromCounterpart(StepSignatures(exchangeInfo.id, 1, dummySig, dummySig))
    val failure = listener.expectMsgClass(classOf[ExchangeFailure])
    failure.lastOffer should be (None)
    failure.cause.isInstanceOf[InvalidStepSignature] should be (true)
    failure.cause.asInstanceOf[InvalidStepSignature].step should be (1)
    failure.cause.asInstanceOf[InvalidStepSignature].cause should be (error)
  }
}
