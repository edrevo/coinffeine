package com.coinffeine.client.micropayment

import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor.Props
import akka.testkit.TestProbe
import com.google.bitcoin.crypto.TransactionSignature

import com.coinffeine.client.CoinffeineClientTest
import com.coinffeine.client.exchange.{MockExchange, SellerUser}
import com.coinffeine.client.micropayment.MicroPaymentChannelActor._
import com.coinffeine.common.Currency.Euro
import com.coinffeine.common.PeerConnection
import com.coinffeine.common.protocol.ProtocolConstants

class SellerMicroPaymentChannelActorFailureTest extends CoinffeineClientTest("buyerExchange") {

  val exchangeInfo = sampleExchangeInfo
  override val broker: PeerConnection = exchangeInfo.broker.connection
  override val counterpart: PeerConnection = exchangeInfo.counterpart.connection
  val protocolConstants = ProtocolConstants(exchangePaymentProofTimeout = 0.5 seconds)
  val exchange = new MockExchange(exchangeInfo) with SellerUser[Euro.type]
  val dummySig = TransactionSignature.dummy

  trait Fixture {
    val listener = TestProbe()
    val actor = system.actorOf(Props[SellerMicroPaymentChannelActor[Euro.type]])
    listener.watch(actor)
  }

  "The seller exchange actor" should "return a failure message if the buyer does not provide the" +
    " necessary payment proof within the specified timeout" in new Fixture{
    actor ! StartMicroPaymentChannel(exchange, protocolConstants, gateway.ref, Set(listener.ref))
    val failure = listener.expectMsgClass(classOf[ExchangeFailure])
    failure.lastOffer should be (None)
    failure.cause.isInstanceOf[TimeoutException] should be (true)
  }
}
