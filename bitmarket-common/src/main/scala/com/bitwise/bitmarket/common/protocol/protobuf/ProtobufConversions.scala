package com.bitwise.bitmarket.common.protocol.protobuf

import java.math.BigDecimal
import java.util.Currency

import com.googlecode.protobuf.pro.duplex.PeerInfo

import com.bitwise.bitmarket.common.PeerConnection
import com.bitwise.bitmarket.common.currency.{BtcAmount, FiatAmount}
import com.bitwise.bitmarket.common.protocol._
import com.bitwise.bitmarket.common.protocol.protobuf.{BitmarketProtobuf => msg}

/** Conversion from/to domain classes and Protobuf messages. */
object ProtobufConversions {

  def fromProtobuf(message: msg.Offer): Offer = Offer(
    id = OfferId(message.getId),
    sequenceNumber = message.getSeq,
    fromId = PeerId(message.getFrom),
    fromConnection = PeerConnection.parse(message.getConnection),
    amount = fromProtobuf(message.getAmount),
    bitcoinPrice = fromProtobuf(message.getBtcPrice)
  )

  def fromProtobuf(message: msg.ExchangeRequest): ExchangeRequest = new ExchangeRequest(
    id = OfferId(message.getId),
    fromId = PeerId(message.getFrom),
    fromConnection = PeerConnection.parse(message.getConnection),
    amount = fromProtobuf(message.getAmount)
  )

  def fromProtobuf(amount: msg.BtcAmount): BtcAmount =
    BtcAmount(BigDecimal.valueOf(amount.getValue, amount.getScale))

  def fromProtobuf(amount: msg.FiatAmount): FiatAmount = FiatAmount(
    BigDecimal.valueOf(amount.getValue, amount.getScale),
    Currency.getInstance(amount.getCurrency)
  )

  def fromProtobuf(order: msg.Order, senderInfo: PeerInfo): Order = {
    val orderConstructor = order.getType match {
      case msg.OrderType.BID => Bid.apply _
      case msg.OrderType.ASK => Ask.apply _
    }
    orderConstructor(
      fromProtobuf(order.getAmount),
      fromProtobuf(order.getPrice),
      senderInfo.toString
    )
  }

  def fromProtobuf(quote: msg.Quote): Quote = {
    val bidOption = if (quote.hasHighestBid) Some(fromProtobuf(quote.getHighestBid)) else None
    val askOption = if (quote.hasLowestAsk) Some(fromProtobuf(quote.getLowestAsk)) else None
    val lastPriceOption = if (quote.hasLastPrice) Some(fromProtobuf(quote.getLastPrice)) else None
    Quote(bidOption -> askOption, lastPriceOption)
  }

  def toProtobuf(offer: Offer): msg.Offer = msg.Offer.newBuilder
    .setId(offer.id.bytes)
    .setSeq(offer.sequenceNumber)
    .setFrom(offer.fromId.address)
    .setConnection(offer.fromConnection.toString)
    .setAmount(toProtobuf(offer.amount))
    .setBtcPrice(toProtobuf(offer.bitcoinPrice))
    .build

  def toProtobuf(acceptance: ExchangeRequest): msg.ExchangeRequest = msg.ExchangeRequest.newBuilder
    .setId(acceptance.id.bytes)
    .setFrom(acceptance.fromId.address)
    .setConnection(acceptance.fromConnection.toString)
    .setAmount(toProtobuf(acceptance.amount))
    .build

  def toProtobuf(amount: BtcAmount): msg.BtcAmount = msg.BtcAmount.newBuilder
    .setValue(amount.amount.underlying().unscaledValue.longValue)
    .setScale(amount.amount.scale)
    .build

  def toProtobuf(amount: FiatAmount): msg.FiatAmount = msg.FiatAmount.newBuilder
    .setValue(amount.amount.underlying().unscaledValue.longValue)
    .setScale(amount.amount.scale)
    .setCurrency(amount.currency.getCurrencyCode)
    .build

  def toProtobuf(order: Order): msg.Order = msg.Order.newBuilder
    .setType(order match {
      case _: Bid => msg.OrderType.BID
      case _: Ask => msg.OrderType.ASK
    })
    .setAmount(toProtobuf(order.amount))
    .setPrice(toProtobuf(order.price))
    .build

  def toProtobuf(quote: Quote): msg.Quote = {
    val builder = msg.Quote.newBuilder
    val Quote((bidOption, askOption), lastPriceOption) = quote
    bidOption.foreach(bid => builder.setHighestBid(toProtobuf(bid)))
    askOption.foreach(ask => builder.setLowestAsk(toProtobuf(ask)))
    lastPriceOption.foreach(lastPrice => builder.setLastPrice(toProtobuf(lastPrice)))
    builder.build
  }
}
