package com.bitwise.bitmarket.common.protocol.protobuf

import com.google.protobuf.Message
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import com.bitwise.bitmarket.common.PeerConnection
import com.bitwise.bitmarket.common.currency.{FiatAmount, BtcAmount}
import com.bitwise.bitmarket.common.currency.CurrencyCode.EUR
import com.bitwise.bitmarket.common.protocol._
import com.bitwise.bitmarket.common.protocol.protobuf.{BitmarketProtobuf => msg}
import com.bitwise.bitmarket.common.protocol.protobuf.DefaultProtoMappings._

class DefaultProtoMappingsTest extends FlatSpec with ShouldMatchers {

  def thereIsAMappingBetween[T, M <: Message](obj: T, msg: M)(implicit mapping: ProtoMapping[T, M]) {

    it should "convert the case class into the protobuf message" in {
      ProtoMapping.toProtobuf(obj) should be (msg)
    }

    it should "convert to protobuf message to the case class" in {
      ProtoMapping.fromProtobuf(msg) should be (obj)
    }

    it should "convert to protobuf and back again" in {
      ProtoMapping.fromProtobuf(ProtoMapping.toProtobuf(obj)) should be (obj)
    }
  }

  val btcAmount = BtcAmount(1.1)
  val btcAmountMessage = msg.BtcAmount.newBuilder
    .setValue(11)
    .setScale(1)
    .build()
  "BTC amount" should behave like thereIsAMappingBetween(btcAmount, btcAmountMessage)

  "Fiat amount" should behave like thereIsAMappingBetween(EUR(3), msg.FiatAmount.newBuilder
    .setCurrency("EUR")
    .setScale(0)
    .setValue(3)
    .build
  )

  val bidMessage = msg.Order.newBuilder
    .setType(msg.OrderType.BID)
    .setAmount(msg.BtcAmount.newBuilder.setValue(2).setScale(0))
    .setPrice(msg.FiatAmount.newBuilder.setValue(300).setScale(0).setCurrency("EUR"))
    .build
  val askMessage = bidMessage.toBuilder.setType(msg.OrderType.ASK).build
  val bid = Order(Bid, BtcAmount(2), EUR(300))
  val ask = Order(Ask, BtcAmount(2), EUR(300))

  "A bid order" should behave like thereIsAMappingBetween(bid, bidMessage)
  "An ask order" should behave like thereIsAMappingBetween(ask, askMessage)

  val cancellation = OrderCancellation(EUR.currency)
  val cancellationMessage = msg.OrderCancellation.newBuilder.setCurrency("EUR").build
  "Order cancellation" should behave like thereIsAMappingBetween(cancellation, cancellationMessage)

  val exchange = ExchangeRequest(
    exchangeId = "1234567890",
    fromId = PeerId("abcdefghijklmnopqrsruvwxyz"),
    fromConnection = PeerConnection.parse("bitmarket://example.com:1234/"),
    amount = BtcAmount(2)
  )
  val exchangeMessage = msg.ExchangeRequest.newBuilder
    .setId("1234567890")
    .setFrom("abcdefghijklmnopqrsruvwxyz")
    .setConnection("bitmarket://example.com:1234/")
    .setAmount(msg.BtcAmount.newBuilder.setValue(2).setScale(0))
    .build
  "Exchange request" must behave like thereIsAMappingBetween(exchange, exchangeMessage)

  val orderMatch = OrderMatch(
    orderMatchId = "1234",
    amount = BtcAmount(0.1),
    price = EUR(10000),
    buyer = PeerConnection("buyer", 8080),
    seller = PeerConnection("seller", 1234)
  )
  val orderMatchMessage = msg.OrderMatch.newBuilder
    .setOrderMatchId("1234")
    .setAmount(ProtoMapping.toProtobuf[BtcAmount, msg.BtcAmount](BtcAmount(0.1)))
    .setPrice(ProtoMapping.toProtobuf[FiatAmount, msg.FiatAmount](EUR(10000)))
    .setBuyer("bitmarket://buyer:8080/")
    .setSeller("bitmarket://seller:1234/")
    .build
  "Order match" must behave like thereIsAMappingBetween(orderMatch, orderMatchMessage)
}
