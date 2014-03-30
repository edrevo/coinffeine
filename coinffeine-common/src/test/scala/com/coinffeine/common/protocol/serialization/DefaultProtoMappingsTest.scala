package com.coinffeine.common.protocol.serialization

import java.math.BigInteger

import com.google.bitcoin.core.{Transaction, Sha256Hash}
import com.google.bitcoin.crypto.TransactionSignature
import com.google.bitcoin.params.UnitTestParams
import com.google.protobuf.{ByteString, Message}
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.currency.{FiatAmount, BtcAmount}
import com.coinffeine.common.currency.CurrencyCode.EUR
import com.coinffeine.common.network.UnitTestNetworkComponent
import com.coinffeine.common.protocol.messages.arbitration._
import com.coinffeine.common.protocol.messages.brokerage._
import com.coinffeine.common.protocol.messages.handshake._
import com.coinffeine.common.protocol.protobuf.{CoinffeineProtobuf => msg}

class DefaultProtoMappingsTest extends FlatSpec with ShouldMatchers with UnitTestNetworkComponent {

  val commitmentTransaction = new Transaction(network)
  val txSerialization = new TransactionSerialization(network)
  val testMappings = new DefaultProtoMappings(txSerialization)
  import testMappings._

  def thereIsAMappingBetween[T, M <: Message](obj: T, msg: M)
                                             (implicit mapping: ProtoMapping[T, M]) {

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

  val sampleTxId = new Sha256Hash("d03f71f44d97243a83804b227cee881280556e9e73e5110ecdcb1bbf72d75c71")

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

  "Bid order" should behave like thereIsAMappingBetween(bid, bidMessage)
  "Ask order" should behave like thereIsAMappingBetween(ask, askMessage)

  val commitmentNotification = CommitmentNotification(
    exchangeId = "1234",
    buyerTxId = sampleTxId,
    sellerTxId = sampleTxId
  )
  val commitmentNotificationMessage = msg.CommitmentNotification.newBuilder()
    .setExchangeId("1234")
    .setBuyerTxId(ByteString.copyFrom(sampleTxId.getBytes))
    .setSellerTxId(ByteString.copyFrom(sampleTxId.getBytes))
    .build()

  "Commitment notification" should behave like thereIsAMappingBetween(
    commitmentNotification, commitmentNotificationMessage)

  val enterExchange = EnterExchange(exchangeId = "1234", commitmentTransaction)
  val enterExchangeMessage = msg.EnterExchange.newBuilder()
    .setExchangeId("1234")
    .setCommitmentTransaction( txSerialization.serialize(commitmentTransaction))
    .build()

  "Enter exchange" must behave like thereIsAMappingBetween(enterExchange, enterExchangeMessage)

  val exchangeAborted = ExchangeAborted("1234", "a reason")
  val exchangeAbortedMessage = msg.ExchangeAborted.newBuilder()
    .setExchangeId("1234")
    .setReason("a reason")
    .build()

  "Exchange aborted" should behave like thereIsAMappingBetween(
    exchangeAborted, exchangeAbortedMessage)

  val exchangeRejection = ExchangeRejection(
    exchangeId = "1234",
    reason = "a reason")
  val exchangeRejectionMessage = msg.ExchangeRejection.newBuilder()
    .setExchangeId("1234")
    .setReason("a reason")
    .build()

  "Exchange rejection" should behave like thereIsAMappingBetween(
    exchangeRejection, exchangeRejectionMessage)

  val cancellation = OrderCancellation(EUR.currency)
  val cancellationMessage = msg.OrderCancellation.newBuilder.setCurrency("EUR").build

  "Order cancellation" should behave like thereIsAMappingBetween(cancellation, cancellationMessage)

  val orderMatch = OrderMatch(
    exchangeId = "1234",
    amount = BtcAmount(0.1),
    price = EUR(10000),
    buyer = PeerConnection("buyer", 8080),
    seller = PeerConnection("seller", 1234)
  )
  val orderMatchMessage = msg.OrderMatch.newBuilder
    .setExchangeId("1234")
    .setAmount(ProtoMapping.toProtobuf[BtcAmount, msg.BtcAmount](BtcAmount(0.1)))
    .setPrice(ProtoMapping.toProtobuf[FiatAmount, msg.FiatAmount](EUR(10000)))
    .setBuyer("coinffeine://buyer:8080/")
    .setSeller("coinffeine://seller:1234/")
    .build
  "Order match" must behave like thereIsAMappingBetween(orderMatch, orderMatchMessage)

  val emptyQuoteMessage = msg.Quote.newBuilder.setCurrency(EUR.currency.getCurrencyCode).build
  val emptyQuote = Quote.empty(EUR.currency)
  "Empty quota" must behave like thereIsAMappingBetween(emptyQuote, emptyQuoteMessage)

  val quoteMessage = emptyQuoteMessage.toBuilder
    .setHighestBid(ProtoMapping.toProtobuf[FiatAmount, msg.FiatAmount](EUR(20)))
    .setLowestAsk(ProtoMapping.toProtobuf[FiatAmount, msg.FiatAmount](EUR(30)))
    .setLastPrice(ProtoMapping.toProtobuf[FiatAmount, msg.FiatAmount](EUR(22)))
    .build
  val quote = Quote(EUR(20) -> EUR(30), EUR(22))
  "Quota" must behave like thereIsAMappingBetween(quote, quoteMessage)

  val quoteRequest = QuoteRequest(EUR.currency)
  val quoteRequestMessage = msg.QuoteRequest.newBuilder
    .setCurrency("EUR")
    .build

  "Quote request" must behave like thereIsAMappingBetween(quoteRequest, quoteRequestMessage)

  val refundTx = new Transaction(UnitTestParams.get())
  val refundTxSignatureRequest = RefundTxSignatureRequest(exchangeId = "1234", refundTx = refundTx)
  val refundTxSignatureRequestMessage = msg.RefundTxSignatureRequest.newBuilder()
    .setExchangeId("1234")
    .setRefundTx(ByteString.copyFrom(refundTx.bitcoinSerialize()))
    .build()

  "Refund TX signature request" must behave like thereIsAMappingBetween(
    refundTxSignatureRequest, refundTxSignatureRequestMessage)

  val refundTxSignature = new TransactionSignature(BigInteger.ZERO, BigInteger.ZERO)
  val refundTxSignatureResponse = RefundTxSignatureResponse(
    exchangeId = "1234",
    refundSignature = refundTxSignature
  )
  val refundTxSignatureResponseMessage = msg.RefundTxSignatureResponse.newBuilder()
    .setExchangeId("1234")
    .setTransactionSignature(ByteString.copyFrom(refundTxSignature.encodeToBitcoin()))
    .build()

  "Refund TX signature response" must behave like thereIsAMappingBetween(
    refundTxSignatureResponse, refundTxSignatureResponseMessage)
}
