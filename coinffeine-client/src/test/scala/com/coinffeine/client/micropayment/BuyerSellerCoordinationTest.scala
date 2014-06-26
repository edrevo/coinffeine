package com.coinffeine.client.micropayment

import akka.actor.{Actor, ActorRef, Props}
import akka.testkit.TestProbe
import org.scalatest.mock.MockitoSugar

import com.coinffeine.client.CoinffeineClientTest
import com.coinffeine.client.exchange.MockProtoMicroPaymentChannel
import com.coinffeine.client.micropayment.MicroPaymentChannelActor.{ExchangeSuccess, StartMicroPaymentChannel}
import com.coinffeine.common.PeerConnection
import com.coinffeine.common.Currency.Euro
import com.coinffeine.common.exchange.{BuyerRole, SellerRole}
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.{ForwardMessage, ReceiveMessage}

class BuyerSellerCoordinationTest extends CoinffeineClientTest("buyerExchange") with MockitoSugar {
  val buyerListener = TestProbe()
  val sellerListener = TestProbe()
  val exchangeInfo = sampleExchangeInfo
  val protocolConstants = ProtocolConstants()
  val buyerChannel = new MockProtoMicroPaymentChannel(exchangeInfo)
  val sellerChannel = new MockProtoMicroPaymentChannel(exchangeInfo)

  class MessageForwarder(to: ActorRef) extends Actor {
    override val receive: Receive = {
      case ForwardMessage(msg, dest) => to ! ReceiveMessage(msg, dest)
    }
  }

  object MessageForwarder {
    def apply(name: String, to: ActorRef): ActorRef = system.actorOf(
      Props(new MessageForwarder(to)), name)
  }

  override val broker: PeerConnection = exchangeInfo.broker.connection
  override val counterpart: PeerConnection = exchangeInfo.counterpart.connection
  val buyer = system.actorOf(Props[BuyerMicroPaymentChannelActor[Euro.type]], "buyer-exchange-actor")

  val seller = system.actorOf(Props[SellerMicroPaymentChannelActor[Euro.type]], "seller-exchange-actor")

  "The buyer and seller actors" should "be able to perform an exchange" in {
    buyer ! StartMicroPaymentChannel(
      exchange, BuyerRole, buyerChannel, protocolConstants, MessageForwarder("fw-to-seller", seller), Set(buyerListener.ref))
    seller ! StartMicroPaymentChannel(
      exchange, SellerRole, sellerChannel, protocolConstants, MessageForwarder("fw-to-buyer", buyer), Set(sellerListener.ref))
    buyerListener.expectMsg(ExchangeSuccess)
    sellerListener.expectMsg(ExchangeSuccess)
  }
}
