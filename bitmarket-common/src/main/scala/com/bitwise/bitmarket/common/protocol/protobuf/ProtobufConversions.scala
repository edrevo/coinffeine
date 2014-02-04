package com.bitwise.bitmarket.common.protocol.protobuf

import java.io._
import java.util.Currency

import com.google.protobuf.ByteString
import com.google.bitcoin.core.{Transaction, Sha256Hash}
import com.google.bitcoin.crypto.TransactionSignature

import com.bitwise.bitmarket.common.protocol._
import com.bitwise.bitmarket.common.protocol.protobuf.{BitmarketProtobuf => msg}
import com.bitwise.bitmarket.common.protocol.protobuf.DefaultProtoMappings._

/** Conversion from/to domain classes and Protobuf messages. */
object ProtobufConversions {

  def fromProtobuf(exchangeAborted: msg.ExchangeAbortedOrBuilder): ExchangeAborted = ExchangeAborted(
    exchangeId = exchangeAborted.getExchangeId,
    reason = exchangeAborted.getReason
  )

  def fromProtobuf(rejectExchange: msg.ExchangeRejectionOrBuilder): ExchangeRejection = ExchangeRejection(
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
    RefundTxSignatureRequest = RefundTxSignatureRequest(
      refundTx = fromByteArray(refundTxSignatureRequest.getRefundTx.toByteArray).asInstanceOf[Transaction] ,
      exchangeId = refundTxSignatureRequest.getExchangeId
  )

  def fromProtobuf(enterExchange: msg.EnterExchangeOrBuilder): EnterExchange = EnterExchange(
    commitmentTransaction = fromByteArray(enterExchange.getCommitmentTransaction.toByteArray).asInstanceOf[Transaction],
    exchangeId = enterExchange.getExchangeId
  )

  def fromProtobuf(
      refundTxSignatureResponse: msg.RefundTxSignatureResponseOrBuilder): RefundTxSignatureResponse =
    RefundTxSignatureResponse(
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

  def toProtobuf(quoteRequest: QuoteRequest): msg.QuoteRequest = {
    msg.QuoteRequest.newBuilder
      .setCurrency(quoteRequest.currency.getCurrencyCode)
      .build
  }

  def toProtobuf(quote: Quote): msg.Quote = {
    val builder = msg.Quote.newBuilder
    val Quote((bidOption, askOption), lastPriceOption) = quote
    bidOption.foreach(bid => builder.setHighestBid(ProtoMapping.toProtobuf(bid)))
    askOption.foreach(ask => builder.setLowestAsk(ProtoMapping.toProtobuf(ask)))
    lastPriceOption.foreach(lastPrice => builder.setLastPrice(ProtoMapping.toProtobuf(lastPrice)))
    builder.build
  }

  def toProtobuf(exchangeAborted: ExchangeAborted): msg.ExchangeAborted =
    msg.ExchangeAborted.newBuilder
      .setExchangeId(exchangeAborted.exchangeId)
      .setReason(exchangeAborted.reason)
      .build

  def toProtobuf(rejectExchange: ExchangeRejection): msg.ExchangeRejection = msg.ExchangeRejection.newBuilder
    .setExchangeId(rejectExchange.exchangeId)
    .setReason(rejectExchange.reason)
    .build

  def toProtobuf(commitmentNotification: CommitmentNotification): msg.CommitmentNotification =
    msg.CommitmentNotification.newBuilder
      .setExchangeId(commitmentNotification.exchangeId)
      .setBuyerTxId(ByteString.copyFrom(commitmentNotification.buyerTxId.getBytes))
      .setSellerTxId(ByteString.copyFrom(commitmentNotification.sellerTxId.getBytes))
      .build

  def toProtobuf(refundTxSignatureRequest: RefundTxSignatureRequest): msg.RefundTxSignatureRequest =
    msg.RefundTxSignatureRequest.newBuilder
      .setExchangeId(refundTxSignatureRequest.exchangeId)
      .setRefundTx(ByteString.copyFrom(toByteArray(refundTxSignatureRequest.refundTx)))
      .build

  def toProtobuf(refundTxSignatureResponse: RefundTxSignatureResponse): msg.RefundTxSignatureResponse =
    msg.RefundTxSignatureResponse.newBuilder
      .setExchangeId(refundTxSignatureResponse.exchangeId)
      .setTransactionSignature(ByteString.copyFrom(refundTxSignatureResponse.refundSignature.encodeToBitcoin()))
      .build()

  def toProtobuf(enterExchange: EnterExchange): msg.EnterExchangeOrBuilder = msg.EnterExchange.newBuilder
    .setExchangeId(enterExchange.exchangeId)
    .setCommitmentTransaction(ByteString.copyFrom(toByteArray(enterExchange.commitmentTransaction)))
    .build

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
