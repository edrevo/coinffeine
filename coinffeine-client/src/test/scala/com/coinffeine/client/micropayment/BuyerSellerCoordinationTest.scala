package com.coinffeine.client.micropayment

import akka.actor.{Actor, ActorRef, Props}
import akka.testkit.TestProbe
import org.scalatest.mock.MockitoSugar

import com.coinffeine.client.CoinffeineClientTest
import com.coinffeine.client.exchange.MockProtoMicroPaymentChannel
import com.coinffeine.client.micropayment.MicroPaymentChannelActor.{ExchangeSuccess, StartMicroPaymentChannel}
import com.coinffeine.client.paymentprocessor.MockPaymentProcessorFactory
import com.coinffeine.common.Currency.Euro
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.exchange.{BuyerRole, SellerRole}
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.{ForwardMessage, ReceiveMessage}

class BuyerSellerCoordinationTest extends CoinffeineClientTest("buyerExchange") with MockitoSugar {
  val buyerListener = TestProbe()
  val sellerListener = TestProbe()
  val protocolConstants = ProtocolConstants()
  val buyerChannel = new MockProtoMicroPaymentChannel(exchange)
  val sellerChannel = new MockProtoMicroPaymentChannel(exchange)
  val paymentProcFactory = new MockPaymentProcessorFactory()

  class MessageForwarder(to: ActorRef) extends Actor {
    override val receive: Receive = {
      case ForwardMessage(msg, dest) => to ! ReceiveMessage(msg, dest)
    }
  }

  object MessageForwarder {
    def apply(name: String, to: ActorRef): ActorRef = system.actorOf(
      Props(new MessageForwarder(to)), name)
  }

  val buyerPaymentProc = system.actorOf(paymentProcFactory.newProcessor(
    exchange.buyer.paymentProcessorAccount, Seq(1000.EUR)))
  val buyer = system.actorOf(Props[BuyerMicroPaymentChannelActor[Euro.type]], "buyer-exchange-actor")

  val sellerPaymentProc = system.actorOf(paymentProcFactory.newProcessor(
    exchange.seller.paymentProcessorAccount, Seq(0.EUR)))
  val seller =
    system.actorOf(Props[SellerMicroPaymentChannelActor[Euro.type]], "seller-exchange-actor")

  "The buyer and seller actors" should "be able to perform an exchange" in {
    buyer ! StartMicroPaymentChannel(
      exchange, BuyerRole, buyerChannel, protocolConstants, buyerPaymentProc,
      MessageForwarder("fw-to-seller", seller), Set(buyerListener.ref))
    seller ! StartMicroPaymentChannel(
      exchange, SellerRole, sellerChannel, protocolConstants, sellerPaymentProc,
      MessageForwarder("fw-to-buyer", buyer), Set(sellerListener.ref))
    buyerListener.expectMsg(ExchangeSuccess)
    sellerListener.expectMsg(ExchangeSuccess)
  }
}
