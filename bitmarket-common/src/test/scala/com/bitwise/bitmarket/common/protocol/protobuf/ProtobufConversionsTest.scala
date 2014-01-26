package com.bitwise.bitmarket.common.protocol.protobuf

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import com.bitwise.bitmarket.common.PeerConnection
import com.bitwise.bitmarket.common.currency.BtcAmount
import com.bitwise.bitmarket.common.currency.CurrencyCode.EUR
import com.bitwise.bitmarket.common.protocol._
import com.bitwise.bitmarket.common.protocol.protobuf.{BitmarketProtobuf => msg}

class ProtobufConversionsTest extends FlatSpec with ShouldMatchers {
  import ProtobufConversions._

  val offerMessage = msg.Offer.newBuilder
    .setId("1234567890")
    .setSeq(0)
    .setFrom("abcdefghijklmnopqrsruvwxyz")
    .setConnection("bitmarket://example.com:1234/")
    .setAmount(msg.BtcAmount.newBuilder.setValue(2).setScale(0))
    .setBtcPrice(msg.FiatAmount.newBuilder.setValue(100).setScale(0).setCurrency("EUR"))
    .build
  val offer = Offer(
    id = "1234567890",
    sequenceNumber = 0,
    fromId = PeerId("abcdefghijklmnopqrsruvwxyz"),
    fromConnection = PeerConnection.parse("bitmarket://example.com:1234/"),
    amount = BtcAmount(2),
    bitcoinPrice = EUR(100)
  )

  "An offer" should "be converted from protobuf" in {
    fromProtobuf(offerMessage) should be (offer)
  }

  it should "be converted to protobuf" in {
    toProtobuf(offer) should be (offerMessage)
  }

  it should "be converted to protobuf and back again" in {
    fromProtobuf(toProtobuf(offer)) should be (offer)
  }

  val exchangeMessage = msg.ExchangeRequest.newBuilder
    .setId("1234567890")
    .setFrom("abcdefghijklmnopqrsruvwxyz")
    .setConnection("bitmarket://example.com:1234/")
    .setAmount(msg.BtcAmount.newBuilder.setValue(2).setScale(0))
    .build
  val exchange = ExchangeRequest(
    id = "1234567890",
    fromId = PeerId("abcdefghijklmnopqrsruvwxyz"),
    fromConnection = PeerConnection.parse("bitmarket://example.com:1234/"),
    amount = BtcAmount(2)
  )

  "An exchange" should "be converted from protobuf" in {
    fromProtobuf(exchangeMessage) should be (exchange)
  }

  it should "be converted to protobuf" in {
    toProtobuf(exchange) should be (exchangeMessage)
  }

  it should "be converted to protobuf and back again" in {
    fromProtobuf(toProtobuf(exchange)) should be (exchange)
  }

  val bidMessage = msg.Order.newBuilder
    .setType(msg.OrderType.BID)
    .setAmount(msg.BtcAmount.newBuilder.setValue(2).setScale(0))
    .setPrice(msg.FiatAmount.newBuilder.setValue(300).setScale(0).setCurrency("EUR"))
    .build
  val askMessage = bidMessage.toBuilder.setType(msg.OrderType.ASK).build
  val senderInfo = PeerConnection("host", 8080)
  val bid = Bid(BtcAmount(2), EUR(300), senderInfo.toString)
  val ask = Ask(BtcAmount(2), EUR(300), senderInfo.toString)

  "An order" should "be converted from protobuf" in {
    fromProtobuf(bidMessage, senderInfo) should be (bid)
    fromProtobuf(askMessage, senderInfo) should be (ask)
  }

  it should "be converted to protobuf" in {
    toProtobuf(bid) should be (bidMessage)
    toProtobuf(ask) should be (askMessage)
  }

  it should "be converted to protobuf and back again" in {
    fromProtobuf(toProtobuf(bid), senderInfo) should be (bid)
    fromProtobuf(toProtobuf(ask), senderInfo) should be (ask)
  }

  val quoteMessage = msg.Quote.newBuilder
    .setHighestBid(toProtobuf(EUR(20)))
    .setLowestAsk(toProtobuf(EUR(30)))
    .setLastPrice(toProtobuf(EUR(22)))
    .build
  val emptyQuoteMessage = msg.Quote.newBuilder.build
  val quote = Quote(EUR(20) -> EUR(30), EUR(22))
  val emptyQuote = Quote(None -> None, None)

  "A quote" should "be converted from protobuf" in {
    fromProtobuf(quoteMessage) should be (quote)
    fromProtobuf(emptyQuoteMessage) should be (emptyQuote)
  }

  it should "be converted to protobuf" in {
    toProtobuf(quote) should be (quoteMessage)
  }

  it should "be converted to protobuf and back again" in {
    fromProtobuf(toProtobuf(quote)) should be (quote)
    fromProtobuf(toProtobuf(emptyQuote)) should be (emptyQuote)
  }

  val orderMatchMessage = msg.OrderMatch.newBuilder
    .setId("1234")
    .setAmount(toProtobuf(BtcAmount(0.1)))
    .setPrice(toProtobuf(EUR(10000)))
    .setBuyer("bitmarket://buyer:8080")
    .setSeller("bitmarket://seller:1234")
    .build
  val orderMatch = OrderMatch(
    id = "1234",
    amount = BtcAmount(0.1),
    price = EUR(10000),
    buyer = "bitmarket://buyer:8080",
    seller = "bitmarket://seller:1234"
  )

  "An order match" should "be converted from protobuf" in {
    fromProtobuf(orderMatchMessage) should be (orderMatch)
  }

  it should "be converted to protobuf" in {
    toProtobuf(orderMatch) should be (orderMatchMessage)
  }

  it should "be converted to protobuf and back again" in {
    fromProtobuf(toProtobuf(orderMatch)) should be (orderMatch)
  }
}
