package com.bitwise.bitmarket.common.protocol.protobuf

import com.googlecode.protobuf.pro.duplex.PeerInfo
import org.scalatest.FlatSpec
import org.scalatest.matchers.MustMatchers

import com.bitwise.bitmarket.common.PeerConnection
import com.bitwise.bitmarket.common.currency.CurrencyCode.EUR
import com.bitwise.bitmarket.common.currency.BtcAmount
import com.bitwise.bitmarket.common.protocol._
import com.bitwise.bitmarket.common.protocol.protobuf.{BitmarketProtobuf => msg}

class ProtobufConversionsTest extends FlatSpec with MustMatchers {
  import ProtobufConversions._

  val offerMessage = msg.Offer.newBuilder
    .setId(1234567890)
    .setSeq(0)
    .setFrom("abcdefghijklmnopqrsruvwxyz")
    .setConnection("bitmarket://example.com:1234/")
    .setAmount(msg.BtcAmount.newBuilder.setValue(2).setScale(0))
    .setBtcPrice(msg.FiatAmount.newBuilder.setValue(100).setScale(0).setCurrency("EUR"))
    .build
  val offer = Offer(
    id = new OfferId(1234567890),
    sequenceNumber = 0,
    fromId = PeerId("abcdefghijklmnopqrsruvwxyz"),
    fromConnection = PeerConnection.parse("bitmarket://example.com:1234/"),
    amount = BtcAmount(2),
    bitcoinPrice = EUR(100)
  )

  "An offer" must "be converted from protobuf" in {
    fromProtobuf(offerMessage) must be (offer)
  }

  it must "be converted to protobuf" in {
    toProtobuf(offer) must be (offerMessage)
  }

  it must "be converted to protobuf and back again" in {
    fromProtobuf(toProtobuf(offer)) must be (offer)
  }

  val exchangeMessage = msg.ExchangeRequest.newBuilder
    .setId(1234567890)
    .setFrom("abcdefghijklmnopqrsruvwxyz")
    .setConnection("bitmarket://example.com:1234/")
    .setAmount(msg.BtcAmount.newBuilder.setValue(2).setScale(0))
    .build
  val exchange = ExchangeRequest(
    id = OfferId(1234567890),
    fromId = PeerId("abcdefghijklmnopqrsruvwxyz"),
    fromConnection = PeerConnection.parse("bitmarket://example.com:1234/"),
    amount = BtcAmount(2)
  )

  "An exchange" must "be converted from protobuf" in {
    fromProtobuf(exchangeMessage) must be (exchange)
  }

  it must "be converted to protobuf" in {
    toProtobuf(exchange) must be (exchangeMessage)
  }

  it must "be converted to protobuf and back again" in {
    fromProtobuf(toProtobuf(exchange)) must be (exchange)
  }

  val bidMessage = msg.Order.newBuilder
    .setType(msg.OrderType.BID)
    .setAmount(msg.BtcAmount.newBuilder.setValue(2).setScale(0))
    .setPrice(msg.FiatAmount.newBuilder.setValue(300).setScale(0).setCurrency("EUR"))
    .build
  val askMessage = bidMessage.toBuilder.setType(msg.OrderType.ASK).build
  val senderInfo = new PeerInfo("host", 8080)
  val bid = Bid(BtcAmount(2), EUR(300), senderInfo.toString)
  val ask = Ask(BtcAmount(2), EUR(300), senderInfo.toString)

  "An order" must "be converted from protobuf" in {
    fromProtobuf(bidMessage, senderInfo) must be (bid)
    fromProtobuf(askMessage, senderInfo) must be (ask)
  }

  it must "be converted to protobuf" in {
    toProtobuf(bid) must be (bidMessage)
    toProtobuf(ask) must be (askMessage)
  }

  it must "be converted to protobuf and back again" in {
    fromProtobuf(toProtobuf(bid), senderInfo) must be (bid)
    fromProtobuf(toProtobuf(ask), senderInfo) must be (ask)
  }

  val quoteMessage = msg.Quote.newBuilder
    .setHighestBid(toProtobuf(EUR(20)))
    .setLowestAsk(toProtobuf(EUR(30)))
    .setLastPrice(toProtobuf(EUR(22)))
    .build
  val emptyQuoteMessage = msg.Quote.newBuilder.build
  val quote = Quote(EUR(20) -> EUR(30), EUR(22))
  val emptyQuote = Quote(None -> None, None)

  "A quote" must "be converted from protobuf" in {
    fromProtobuf(quoteMessage) must be (quote)
    fromProtobuf(emptyQuoteMessage) must be (emptyQuote)
  }

  it must "be converted to protobuf" in {
    toProtobuf(quote) must be (quoteMessage)
  }

  it must "be converted to protobuf and back again" in {
    fromProtobuf(toProtobuf(quote)) must be (quote)
    fromProtobuf(toProtobuf(emptyQuote)) must be (emptyQuote)
  }
}
