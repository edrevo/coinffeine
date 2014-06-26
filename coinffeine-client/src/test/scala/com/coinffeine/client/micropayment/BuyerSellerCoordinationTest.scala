package com.coinffeine.client.micropayment

import akka.actor.{Actor, ActorRef, Props}
import akka.testkit.TestProbe
import org.scalatest.mock.MockitoSugar

import com.coinffeine.client.CoinffeineClientTest
import com.coinffeine.client.exchange.MockProtoMicroPaymentChannel
import com.coinffeine.client.micropayment.MicroPaymentChannelActor.{ExchangeSuccess, StartMicroPaymentChannel}
import com.coinffeine.client.paymentprocessor.MockPaymentProcessorFactory
import com.coinffeine.common.PeerConnection
import com.coinffeine.common.Currency.Euro
import com.coinffeine.common.Currency.Implicits._
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

  override val broker: PeerConnection = exchangeInfo.broker.connection
  override val counterpart: PeerConnection = exchangeInfo.counterpart.connection
  val buyerPaymentProc = system.actorOf(paymentProcFactory.newProcessor(
    buyerExchangeInfo.user.paymentProcessorAccount, Seq(1000.EUR)))
  val buyer = system.actorOf(Props[BuyerMicroPaymentChannelActor[Euro.type]], "buyer-exchange-actor")

  val sellerPaymentProc = system.actorOf(paymentProcFactory.newProcessor(
    sellerExchangeInfo.user.paymentProcessorAccount, Seq(0.EUR)))
  val seller = system.actorOf(Props[SellerMicroPaymentChannelActor[Euro.type]], "seller-exchange-actor")

  "The buyer and seller actors" should "be able to perform an exchange" in {
    buyer ! StartMicroPaymentChannel(
      exchange, BuyerRole, buyerChannel, protocolConstants, buyerPaymentProc, MessageForwarder("fw-to-seller", seller), Set(buyerListener.ref))
    seller ! StartMicroPaymentChannel(
      exchange, SellerRole, sellerChannel, protocolConstants, sellerPaymentProc, MessageForwarder("fw-to-buyer", buyer), Set(sellerListener.ref))
    buyerListener.expectMsg(ExchangeSuccess)
    sellerListener.expectMsg(ExchangeSuccess)
  }
}
