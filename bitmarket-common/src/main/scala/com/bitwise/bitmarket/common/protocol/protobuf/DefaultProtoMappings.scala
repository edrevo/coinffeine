package com.bitwise.bitmarket.common.protocol.protobuf

import java.util.Currency

import com.bitwise.bitmarket.common.protocol._
import com.bitwise.bitmarket.common.protocol.protobuf.{BitmarketProtobuf => msg}
import com.bitwise.bitmarket.common.PeerConnection
import com.bitwise.bitmarket.common.currency.{FiatAmount, BtcAmount}
import java.math.BigDecimal

/** Implicit conversion mappings for the protocol messages */
object DefaultProtoMappings {

  implicit val btcAmountMapping = new ProtoMapping[BtcAmount, msg.BtcAmount] {
    override def fromProtobuf(amount: msg.BtcAmount): BtcAmount =
      BtcAmount(BigDecimal.valueOf(amount.getValue, amount.getScale))

    override def toProtobuf(amount: BtcAmount): msg.BtcAmount = msg.BtcAmount.newBuilder
      .setValue(amount.amount.underlying().unscaledValue.longValue)
      .setScale(amount.amount.scale)
      .build
  }

  implicit val fiatAmountMapping = new ProtoMapping[FiatAmount, msg.FiatAmount] {

    override def fromProtobuf(amount: msg.FiatAmount): FiatAmount = FiatAmount(
      BigDecimal.valueOf(amount.getValue, amount.getScale),
      Currency.getInstance(amount.getCurrency)
    )

    override def toProtobuf(amount: FiatAmount): msg.FiatAmount = msg.FiatAmount.newBuilder
      .setValue(amount.amount.underlying().unscaledValue.longValue)
      .setScale(amount.amount.scale)
      .setCurrency(amount.currency.getCurrencyCode)
      .build
  }

  implicit val orderMapping = new ProtoMapping[Order, msg.Order] {

    override def fromProtobuf(order: msg.Order): Order = {
      Order(
        orderType = order.getType match {
          case msg.OrderType.BID => Bid
          case msg.OrderType.ASK => Ask
        },
        amount = ProtoMapping.fromProtobuf(order.getAmount),
        price = ProtoMapping.fromProtobuf(order.getPrice)
      )
    }

    override def toProtobuf(order: Order): msg.Order = msg.Order.newBuilder
      .setType(order.orderType match {
        case Bid => msg.OrderType.BID
        case Ask => msg.OrderType.ASK
      })
      .setAmount(ProtoMapping.toProtobuf(order.amount))
      .setPrice(ProtoMapping.toProtobuf(order.price))
      .build
  }

  implicit val orderCancellationMapping = new ProtoMapping[OrderCancellation, msg.OrderCancellation] {
    override def fromProtobuf(message: msg.OrderCancellation) = OrderCancellation(
      currency = Currency.getInstance(message.getCurrency)
    )
    override def toProtobuf(obj: OrderCancellation) = msg.OrderCancellation.newBuilder
      .setCurrency(obj.currency.getSymbol)
      .build
  }

  implicit val exchangeRequestMapping = new ProtoMapping[ExchangeRequest, msg.ExchangeRequest] {

    override def fromProtobuf(message: msg.ExchangeRequest): ExchangeRequest = ExchangeRequest(
      exchangeId = message.getId,
      fromId = PeerId(message.getFrom),
      fromConnection = PeerConnection.parse(message.getConnection),
      amount = ProtoMapping.fromProtobuf(message.getAmount)
    )

    override def toProtobuf(acceptance: ExchangeRequest): msg.ExchangeRequest =
      msg.ExchangeRequest.newBuilder
        .setId(acceptance.exchangeId)
        .setFrom(acceptance.fromId.address)
        .setConnection(acceptance.fromConnection.toString)
        .setAmount(ProtoMapping.toProtobuf(acceptance.amount))
        .build
  }
}
