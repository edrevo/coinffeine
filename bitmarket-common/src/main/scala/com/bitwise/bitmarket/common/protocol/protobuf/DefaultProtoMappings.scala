package com.bitwise.bitmarket.common.protocol.protobuf

import java.io._
import java.math.BigDecimal
import java.util.Currency

import com.bitwise.bitmarket.common.protocol._
import com.bitwise.bitmarket.common.protocol.protobuf.{BitmarketProtobuf => msg}
import com.bitwise.bitmarket.common.PeerConnection
import com.bitwise.bitmarket.common.currency.{FiatAmount, BtcAmount}
import com.google.bitcoin.core.{Transaction, Sha256Hash}
import com.google.bitcoin.crypto.TransactionSignature
import com.google.protobuf.ByteString

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

  implicit val commitmentNotificationMapping =
    new ProtoMapping[CommitmentNotification, msg.CommitmentNotification] {

      override def fromProtobuf(commitment: msg.CommitmentNotification) = CommitmentNotification(
        exchangeId = commitment.getExchangeId,
        buyerTxId = new Sha256Hash(commitment.getBuyerTxId.toByteArray),
        sellerTxId = new Sha256Hash(commitment.getSellerTxId.toByteArray)
      )

      override def toProtobuf(commitment: CommitmentNotification) = msg.CommitmentNotification.newBuilder
        .setExchangeId(commitment.exchangeId)
        .setBuyerTxId(ByteString.copyFrom(commitment.buyerTxId.getBytes))
        .setSellerTxId(ByteString.copyFrom(commitment.sellerTxId.getBytes))
        .build
    }

  implicit val enterExchangeMapping = new ProtoMapping[EnterExchange, msg.EnterExchange] {

      override def fromProtobuf(enter: msg.EnterExchange) = EnterExchange(
        commitmentTransaction = fromByteArray(
          enter.getCommitmentTransaction.toByteArray).asInstanceOf[Transaction],
        exchangeId = enter.getExchangeId
      )

      override def toProtobuf(enter: EnterExchange) = msg.EnterExchange.newBuilder
        .setExchangeId(enter.exchangeId)
        .setCommitmentTransaction(ByteString.copyFrom(toByteArray(enter.commitmentTransaction)))
        .build
    }

  implicit val exchangeAbortedMapping = new ProtoMapping[ExchangeAborted, msg.ExchangeAborted] {

    override def fromProtobuf(exchangeAborted: msg.ExchangeAborted) = ExchangeAborted(
      exchangeId = exchangeAborted.getExchangeId,
      reason = exchangeAborted.getReason
    )

    override def toProtobuf(exchangeAborted: ExchangeAborted) = msg.ExchangeAborted.newBuilder
      .setExchangeId(exchangeAborted.exchangeId)
      .setReason(exchangeAborted.reason)
      .build
  }

  implicit val exchangeRejectionMapping = new ProtoMapping[ExchangeRejection, msg.ExchangeRejection] {

    override def fromProtobuf(rejection: msg.ExchangeRejection) = ExchangeRejection(
      exchangeId = rejection.getExchangeId,
      reason = rejection.getReason
    )

    override def toProtobuf(rejection: ExchangeRejection) = msg.ExchangeRejection.newBuilder
      .setExchangeId(rejection.exchangeId)
      .setReason(rejection.reason)
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
      .setCurrency(obj.currency.getCurrencyCode)
      .build
  }

  implicit val orderMatchMapping = new ProtoMapping[OrderMatch, msg.OrderMatch] {

    override def fromProtobuf(orderMatch: msg.OrderMatch): OrderMatch = OrderMatch(
      exchangeId = orderMatch.getExchangeId,
      amount = ProtoMapping.fromProtobuf(orderMatch.getAmount),
      price = ProtoMapping.fromProtobuf(orderMatch.getPrice),
      buyer = PeerConnection.parse(orderMatch.getBuyer),
      seller = PeerConnection.parse(orderMatch.getSeller)
    )

    override def toProtobuf(orderMatch: OrderMatch): msg.OrderMatch = msg.OrderMatch.newBuilder
      .setExchangeId(orderMatch.exchangeId)
      .setAmount(ProtoMapping.toProtobuf(orderMatch.amount))
      .setPrice(ProtoMapping.toProtobuf(orderMatch.price))
      .setBuyer(orderMatch.buyer.toString)
      .setSeller(orderMatch.seller.toString)
      .build
  }

  implicit val quoteMapping = new ProtoMapping[Quote, msg.Quote] {

    override def fromProtobuf(quote: msg.Quote): Quote = {
      val bidOption =
        if (quote.hasHighestBid) Some(ProtoMapping.fromProtobuf(quote.getHighestBid)) else None
      val askOption =
        if (quote.hasLowestAsk) Some(ProtoMapping.fromProtobuf(quote.getLowestAsk)) else None
      val lastPriceOption =
        if (quote.hasLastPrice) Some(ProtoMapping.fromProtobuf(quote.getLastPrice)) else None
      Quote(bidOption -> askOption, lastPriceOption)
    }

    override def toProtobuf(quote: Quote): msg.Quote = {
      val builder = msg.Quote.newBuilder
      val Quote((bidOption, askOption), lastPriceOption) = quote
      bidOption.foreach(bid => builder.setHighestBid(ProtoMapping.toProtobuf(bid)))
      askOption.foreach(ask => builder.setLowestAsk(ProtoMapping.toProtobuf(ask)))
      lastPriceOption.foreach(lastPrice => builder.setLastPrice(ProtoMapping.toProtobuf(lastPrice)))
      builder.build
    }
  }

  implicit val quoteRequestMapping = new ProtoMapping[QuoteRequest, msg.QuoteRequest] {

    override def fromProtobuf(request: msg.QuoteRequest): QuoteRequest =
      QuoteRequest(Currency.getInstance(request.getCurrency))

    override def toProtobuf(request: QuoteRequest): msg.QuoteRequest = msg.QuoteRequest.newBuilder
      .setCurrency(request.currency.getCurrencyCode)
      .build
  }

  implicit val refundTxSignatureRequestMapping =
    new ProtoMapping[RefundTxSignatureRequest, msg.RefundTxSignatureRequest] {

      override def fromProtobuf(request: msg.RefundTxSignatureRequest) = RefundTxSignatureRequest(
        refundTx = fromByteArray(request.getRefundTx.toByteArray).asInstanceOf[Transaction],
        exchangeId = request.getExchangeId
      )

      override def toProtobuf(request: RefundTxSignatureRequest) =
        msg.RefundTxSignatureRequest.newBuilder
          .setExchangeId(request.exchangeId)
          .setRefundTx(ByteString.copyFrom(toByteArray(request.refundTx)))
          .build
    }

  implicit val refundTxSignatureResponseMapping =
    new ProtoMapping[RefundTxSignatureResponse, msg.RefundTxSignatureResponse] {

      override def fromProtobuf(response: msg.RefundTxSignatureResponse) = RefundTxSignatureResponse(
        exchangeId = response.getExchangeId,
        refundSignature = TransactionSignature.decodeFromBitcoin(
          response.getTransactionSignature.toByteArray, false)
      )

      override def toProtobuf(response: RefundTxSignatureResponse) =
        msg.RefundTxSignatureResponse.newBuilder
          .setExchangeId(response.exchangeId)
          .setTransactionSignature(ByteString.copyFrom(response.refundSignature.encodeToBitcoin()))
          .build()
    }

  private def toByteArray(obj: AnyRef): Array[Byte] = {
    val byteArrayOutputStream = new ByteArrayOutputStream
    val objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)
    objectOutputStream.writeObject(obj)
    byteArrayOutputStream.toByteArray
  }

  private def fromByteArray[T](bytes: Array[Byte]) = {
    val inputStream: ByteArrayInputStream = new ByteArrayInputStream(bytes)
    val objectInputStream: ObjectInputStream = new ObjectInputStream(inputStream)
    objectInputStream.readObject().asInstanceOf[T]
  }
}
