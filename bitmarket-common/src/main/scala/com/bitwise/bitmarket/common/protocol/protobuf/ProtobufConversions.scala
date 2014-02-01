package com.bitwise.bitmarket.common.protocol.protobuf

import java.io._
import java.math.BigDecimal
import java.util.Currency

import com.google.protobuf.ByteString
import com.google.bitcoin.core.{Transaction, Sha256Hash}
import com.google.bitcoin.crypto.TransactionSignature

import com.bitwise.bitmarket.common.{protocol, PeerConnection}
import com.bitwise.bitmarket.common.currency.{BtcAmount, FiatAmount}
import com.bitwise.bitmarket.common.protocol._
import com.bitwise.bitmarket.common.protocol.protobuf.{BitmarketProtobuf => msg}

/** Conversion from/to domain classes and Protobuf messages. */
object ProtobufConversions {

  def fromProtobuf(message: msg.OfferOrBuilder): Offer = Offer(
    id = message.getId.toString,
    sequenceNumber = message.getSeq,
    fromId = PeerId(message.getFrom),
    fromConnection = PeerConnection.parse(message.getConnection),
    amount = fromProtobuf(message.getAmount),
    bitcoinPrice = fromProtobuf(message.getBtcPrice)
  )

  def fromProtobuf(message: msg.ExchangeRequestOrBuilder): ExchangeRequest = ExchangeRequest(
    exchangeId = message.getId.toString,
    fromId = PeerId(message.getFrom),
    fromConnection = PeerConnection.parse(message.getConnection),
    amount = fromProtobuf(message.getAmount)
  )

  def fromProtobuf(amount: msg.BtcAmountOrBuilder): BtcAmount =
    BtcAmount(BigDecimal.valueOf(amount.getValue, amount.getScale))

  def fromProtobuf(amount: msg.FiatAmountOrBuilder): FiatAmount = FiatAmount(
    BigDecimal.valueOf(amount.getValue, amount.getScale),
    Currency.getInstance(amount.getCurrency)
  )

  def fromProtobuf(orderMatch: msg.OrderMatchOrBuilder): OrderMatch = OrderMatch(
    orderMatchId = orderMatch.getOrderMatchId,
    amount = fromProtobuf(orderMatch.getAmount),
    price = fromProtobuf(orderMatch.getPrice),
    buyer = PeerConnection.parse(orderMatch.getBuyer),
    seller = PeerConnection.parse(orderMatch.getSeller)
  )

  def fromProtobuf(order: msg.OrderOrBuilder, sender: PeerConnection): Order = {
    val orderConstructor = order.getType match {
      case msg.OrderType.BID => Bid.apply _
      case msg.OrderType.ASK => Ask.apply _
    }
    orderConstructor(
      fromProtobuf(order.getAmount),
      fromProtobuf(order.getPrice),
      sender
    )
  }

  def fromProtobuf(crossNotification: msg.CrossNotificationOrBuilder): CrossNotification = CrossNotification(
    exchangeId = crossNotification.getExchangeId,
    cross = fromProtobuf(crossNotification.getCross)
  )

  def fromProtobuf(exchangeAborted: msg.ExchangeAbortedOrBuilder): ExchangeAborted = ExchangeAborted(
    exchangeId = exchangeAborted.getExchangeId,
    reason = exchangeAborted.getReason
  )

  def fromProtobuf(rejectExchange: msg.RejectExchangeOrBuilder): RejectExchange = RejectExchange(
    exchangeId = rejectExchange.getExchangeId,
    reason = rejectExchange.getReason
  )

  def fromProtobuf(commitmentNotification: msg.CommitmentNotificationOrBuilder):
      CommitmentNotification = CommitmentNotification(
      exchangeId = commitmentNotification.getExchangeId,
      buyerTxId = new Sha256Hash(commitmentNotification.getBuyerTxId.toByteArray),
      sellerTxId = new Sha256Hash(commitmentNotification.getSellerTxId.toByteArray)
  )

  def fromProtobuf(refundTxSignatureRequest: msg.RefundTxSignatureRequestOrBuilder):
    protocol.RefundTxSignatureRequest = RefundTxSignatureRequest(
      refundTx = fromByteArray(refundTxSignatureRequest.getRefundTx.toByteArray).asInstanceOf[Transaction] ,
      exchangeId = refundTxSignatureRequest.getExchangeId
  )

  def fromProtobuf(enterExchange: msg.EnterExchangeOrBuilder):
  protocol.EnterExchange = EnterExchange(
    commitmentTransaction = fromByteArray(enterExchange.getCommitmentTransaction.toByteArray).asInstanceOf[Transaction],
    exchangeId = enterExchange.getExchangeId
  )

  def fromProtobuf(refundTxSignatureResponse: msg.RefundTxSignatureResponseOrBuilder):
    protocol.RefundTxSignatureResponse = RefundTxSignatureResponse(
    exchangeId = refundTxSignatureResponse.getExchangeId,
    refundSignature = TransactionSignature.decodeFromBitcoin(
      refundTxSignatureResponse.getTransactionSignature.toByteArray, false)
  )

  def fromProtobuf(quoteRequest: msg.QuoteRequestOrBuilder): QuoteRequest =
    QuoteRequest(Currency.getInstance(quoteRequest.getCurrency))

  def fromProtobuf(quote: msg.QuoteOrBuilder): Quote = {
    val bidOption = if (quote.hasHighestBid) Some(fromProtobuf(quote.getHighestBid)) else None
    val askOption = if (quote.hasLowestAsk) Some(fromProtobuf(quote.getLowestAsk)) else None
    val lastPriceOption = if (quote.hasLastPrice) Some(fromProtobuf(quote.getLastPrice)) else None
    Quote(bidOption -> askOption, lastPriceOption)
  }

  def toProtobuf(offer: Offer): msg.Offer = msg.Offer.newBuilder
    .setId(offer.id)
    .setSeq(offer.sequenceNumber)
    .setFrom(offer.fromId.address)
    .setConnection(offer.fromConnection.toString)
    .setAmount(toProtobuf(offer.amount))
    .setBtcPrice(toProtobuf(offer.bitcoinPrice))
    .build

  def toProtobuf(acceptance: ExchangeRequest): msg.ExchangeRequest = msg.ExchangeRequest.newBuilder
    .setId(acceptance.exchangeId)
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

  def toProtobuf(quoteRequest: QuoteRequest): msg.QuoteRequest = {
    val builder = msg.QuoteRequest.newBuilder
    builder.setCurrency(quoteRequest.currency.getSymbol)
    builder.build
  }

  def toProtobuf(quote: Quote): msg.Quote = {
    val builder = msg.Quote.newBuilder
    val Quote((bidOption, askOption), lastPriceOption) = quote
    bidOption.foreach(bid => builder.setHighestBid(toProtobuf(bid)))
    askOption.foreach(ask => builder.setLowestAsk(toProtobuf(ask)))
    lastPriceOption.foreach(lastPrice => builder.setLastPrice(toProtobuf(lastPrice)))
    builder.build
  }

  def toProtobuf(orderMatch: OrderMatch): msg.OrderMatch = {
    val builder = msg.OrderMatch.newBuilder
    builder.setOrderMatchId(orderMatch.orderMatchId)
    builder.setAmount(toProtobuf(orderMatch.amount))
    builder.setPrice(toProtobuf(orderMatch.price))
    builder.setBuyer(orderMatch.buyer.toString)
    builder.setSeller(orderMatch.seller.toString)
    builder.build
  }

  def toProtobuf(crossNotification: CrossNotification): msg.CrossNotification = {
    val builder = msg.CrossNotification.newBuilder()
    builder.setExchangeId(crossNotification.exchangeId)
    builder.setCross(toProtobuf(crossNotification.cross))
    builder.build
  }

  def toProtobuf(exchangeAborted: ExchangeAborted): msg.ExchangeAborted = {
    val builder = msg.ExchangeAborted.newBuilder()
    builder.setExchangeId(exchangeAborted.exchangeId)
    builder.setReason(exchangeAborted.reason)
    builder.build
  }

  def toProtobuf(rejectExchange: protocol.RejectExchange): msg.RejectExchange = {
    val builder = msg.RejectExchange.newBuilder()
    builder.setExchangeId(rejectExchange.exchangeId)
    builder.setReason(rejectExchange.reason)
    builder.build
  }

  def toProtobuf(commitmentNotification: CommitmentNotification): msg.CommitmentNotification = {
    val builder = msg.CommitmentNotification.newBuilder()
    builder.setExchangeId(commitmentNotification.exchangeId)
    builder.setBuyerTxId(ByteString.copyFrom(commitmentNotification.buyerTxId.getBytes))
    builder.setSellerTxId(ByteString.copyFrom(commitmentNotification.sellerTxId.getBytes))
    builder.build
  }

  def toProtobuf(refundTxSignatureRequest: RefundTxSignatureRequest): msg.RefundTxSignatureRequest = {
    val builder = msg.RefundTxSignatureRequest.newBuilder()
    builder.setExchangeId(refundTxSignatureRequest.exchangeId)
    builder.setRefundTx(ByteString.copyFrom(toByteArray(refundTxSignatureRequest.refundTx)))
    builder.build
  }

  def toProtobuf(refundTxSignatureResponse: RefundTxSignatureResponse): msg.RefundTxSignatureResponse = {
    val builder = msg.RefundTxSignatureResponse.newBuilder()
    builder.setExchangeId(refundTxSignatureResponse.exchangeId)
    builder.setTransactionSignature(ByteString.copyFrom(refundTxSignatureResponse.refundSignature.encodeToBitcoin()))
    builder.build()
  }

  def toProtobuf(enterExchange: EnterExchange): msg.EnterExchangeOrBuilder = {
    val builder = msg.EnterExchange.newBuilder()
    builder.setExchangeId(enterExchange.exchangeId)
    builder.setCommitmentTransaction(ByteString.copyFrom(toByteArray(enterExchange.commitmentTransaction)))
    builder.build
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
