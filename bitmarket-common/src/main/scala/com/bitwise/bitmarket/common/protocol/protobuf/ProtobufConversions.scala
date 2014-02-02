package com.bitwise.bitmarket.common.protocol.protobuf

import java.io._
import java.util.Currency

import com.google.protobuf.ByteString
import com.google.bitcoin.core.{Transaction, Sha256Hash}
import com.google.bitcoin.crypto.TransactionSignature

import com.bitwise.bitmarket.common.{protocol, PeerConnection}
import com.bitwise.bitmarket.common.protocol._
import com.bitwise.bitmarket.common.protocol.protobuf.{BitmarketProtobuf => msg}
import com.bitwise.bitmarket.common.protocol.protobuf.DefaultProtoMappings._

/** Conversion from/to domain classes and Protobuf messages. */
object ProtobufConversions {

  def fromProtobuf(crossNotification: msg.CrossNotificationOrBuilder): CrossNotification = CrossNotification(
    exchangeId = crossNotification.getExchangeId,
    cross = ProtoMapping.fromProtobuf(crossNotification.getCross)
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
    val bidOption = if (quote.hasHighestBid) Some(ProtoMapping.fromProtobuf(quote.getHighestBid)) else None
    val askOption = if (quote.hasLowestAsk) Some(ProtoMapping.fromProtobuf(quote.getLowestAsk)) else None
    val lastPriceOption = if (quote.hasLastPrice) Some(ProtoMapping.fromProtobuf(quote.getLastPrice)) else None
    Quote(bidOption -> askOption, lastPriceOption)
  }

  def toProtobuf(acceptance: ExchangeRequest): msg.ExchangeRequest = msg.ExchangeRequest.newBuilder
    .setId(acceptance.exchangeId)
    .setFrom(acceptance.fromId.address)
    .setConnection(acceptance.fromConnection.toString)
    .setAmount(ProtoMapping.toProtobuf(acceptance.amount))
    .build

  def toProtobuf(quoteRequest: QuoteRequest): msg.QuoteRequest = {
    val builder = msg.QuoteRequest.newBuilder
    builder.setCurrency(quoteRequest.currency.getSymbol)
    builder.build
  }

  def toProtobuf(quote: Quote): msg.Quote = {
    val builder = msg.Quote.newBuilder
    val Quote((bidOption, askOption), lastPriceOption) = quote
    bidOption.foreach(bid => builder.setHighestBid(ProtoMapping.toProtobuf(bid)))
    askOption.foreach(ask => builder.setLowestAsk(ProtoMapping.toProtobuf(ask)))
    lastPriceOption.foreach(lastPrice => builder.setLastPrice(ProtoMapping.toProtobuf(lastPrice)))
    builder.build
  }

  def toProtobuf(crossNotification: CrossNotification): msg.CrossNotification = {
    val builder = msg.CrossNotification.newBuilder()
    builder.setExchangeId(crossNotification.exchangeId)
    builder.setCross(ProtoMapping.toProtobuf(crossNotification.cross))
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
