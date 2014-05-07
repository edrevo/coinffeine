package com.coinffeine.client.exchange

import org.scalatest.mock.MockitoSugar

import scala.concurrent.duration._

import akka.actor.{ActorRef, Actor, Props}
import akka.testkit.TestProbe

import com.coinffeine.client.CoinffeineClientTest
import com.coinffeine.client.exchange.ExchangeActor.{ExchangeSuccess, StartExchange}
import com.coinffeine.common.PeerConnection
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.{ReceiveMessage, ForwardMessage}

class BuyerSellerCoordinationTest extends CoinffeineClientTest("buyerExchange") with MockitoSugar {
  val buyerListener = TestProbe()
  val sellerListener = TestProbe()
  val exchangeInfo = sampleExchangeInfo
  val protocolConstants = ProtocolConstants(
    commitmentConfirmations = 1,
    resubmitRefundSignatureTimeout = 1 second,
    refundSignatureAbortTimeout = 1 minute)
  val buyerExchange = new MockExchange(exchangeInfo) with BuyerUser
  val sellerExchange = new MockExchange(exchangeInfo) with SellerUser

  class MessageForwarder(to: ActorRef) extends Actor {
    override val receive: Receive = {
      case ForwardMessage(msg, dest) => to ! ReceiveMessage(msg, dest)
    }
  }

  object MessageForwarder {
    def apply(name: String, to: ActorRef): ActorRef = system.actorOf(
      Props(new MessageForwarder(to)), name)
  }

  override val broker: PeerConnection = exchangeInfo.broker
  override val counterpart: PeerConnection = exchangeInfo.counterpart
  val buyer = system.actorOf(
    Props(new BuyerExchangeActor(buyerExchange, protocolConstants)),
    "buyer-exchange-actor"
  )

  val seller = system.actorOf(
    Props(new SellerExchangeActor(sellerExchange, protocolConstants)),
    "seller-exchange-actor"
  )

  "The buyer and seller actors" should "be able to perform an exchange" in {
    buyer ! StartExchange(MessageForwarder("fw-to-seller", seller), Set(buyerListener.ref))
    seller ! StartExchange(MessageForwarder("fw-to-buyer", buyer), Set(sellerListener.ref))
    buyerListener.expectMsg(ExchangeSuccess)
    sellerListener.expectMsg(ExchangeSuccess)
  }
}
