package com.bitwise.bitmarket.common.protocol.protobuf

import java.math.BigInteger

import com.google.bitcoin.core.{NetworkParameters, Sha256Hash, Transaction}
import com.google.bitcoin.crypto.TransactionSignature
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar

import com.bitwise.bitmarket.common.PeerConnection
import com.bitwise.bitmarket.common.currency.{FiatAmount, BtcAmount}
import com.bitwise.bitmarket.common.currency.CurrencyCode.EUR
import com.bitwise.bitmarket.common.protocol._
import com.bitwise.bitmarket.common.protocol.protobuf.DefaultProtoMappings._
import com.bitwise.bitmarket.common.protocol.protobuf.{BitmarketProtobuf => msg}

class ProtobufConversionsTest extends FlatSpec with ShouldMatchers with MockitoSugar {
  import ProtobufConversions._

  val quoteRequestMessage = msg.QuoteRequest.newBuilder
    .setCurrency("EUR")
    .build
  val quoteRequest = QuoteRequest(EUR.currency)

  "A quote request" should "be converted from protobuf" in {
    fromProtobuf(quoteRequestMessage) should be (quoteRequest)
  }

  it should "be converted to protobuf" in {
    toProtobuf(quoteRequest) should be (quoteRequestMessage)
  }

  it should "be converted to protobuf and back again" in {
    fromProtobuf(toProtobuf(quoteRequest)) should be (quoteRequest)
  }

  val quoteMessage = msg.Quote.newBuilder
    .setHighestBid(ProtoMapping.toProtobuf[FiatAmount, msg.FiatAmount](EUR(20)))
    .setLowestAsk(ProtoMapping.toProtobuf[FiatAmount, msg.FiatAmount](EUR(30)))
    .setLastPrice(ProtoMapping.toProtobuf[FiatAmount, msg.FiatAmount](EUR(22)))
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
    .setOrderMatchId("1234")
    .setAmount(ProtoMapping.toProtobuf[BtcAmount, msg.BtcAmount](BtcAmount(0.1)))
    .setPrice(ProtoMapping.toProtobuf[FiatAmount, msg.FiatAmount](EUR(10000)))
    .setBuyer("bitmarket://buyer:8080/")
    .setSeller("bitmarket://seller:1234/")
    .build
  val orderMatch = OrderMatch(
    orderMatchId = "1234",
    amount = BtcAmount(0.1),
    price = EUR(10000),
    buyer = PeerConnection("buyer", 8080),
    seller = PeerConnection("seller", 1234)
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

  "An Cross Notification" should "be converted to protobuf and back again" in {
    val crossNotification = CrossNotification(
      exchangeId = "1234",
      cross = orderMatch
    )
    fromProtobuf(toProtobuf(crossNotification)) should be (crossNotification)
  }

  "An Exchange Aborted" should "be converted to protobuf and back again" in {
    val exchangeAborted = ExchangeAborted("1234", "a reason")
    fromProtobuf(toProtobuf(exchangeAborted)) should be (exchangeAborted)
  }

  "An Reject Exchange" should "be converted to protobuf and back again" in {
    val rejectExchange = RejectExchange(
      exchangeId = "1234",
      reason = "a reason")
    fromProtobuf(toProtobuf(rejectExchange)) should be (rejectExchange)
  }

  "An Commitment Notification" should "be converted to protobuf and back again" in {
    val commitmentNotification = CommitmentNotification(
      exchangeId = "exchangeId",
      buyerTxId = new Sha256Hash("d03f71f44d97243a83804b227cee881280556e9e73e5110ecdcb1bbf72d75c71"),
      sellerTxId = new Sha256Hash("d03f71f44d97243a83804b227cee881280556e9e73e5110ecdcb1bbf72d75c71")
    )
    fromProtobuf(toProtobuf(commitmentNotification)) should be (commitmentNotification)
  }

  "An Refund Transaction Signature Request" should "be converted to protobuf and back again" in {
    val refundTxSignatureRequest = RefundTxSignatureRequest(
      exchangeId = "exchangeId",
      refundTx = new Transaction(new NetworkParameters {})
    )
    fromProtobuf(toProtobuf(refundTxSignatureRequest)) should be (refundTxSignatureRequest)
  }

  "An Refund Transaction Signature" should "be converted to protobuf and back again" in {
    val refundTxSignatureResponse = RefundTxSignatureResponse(
      exchangeId = "exchangeId",
      refundSignature = new TransactionSignature(BigInteger.ZERO, BigInteger.ZERO)
    )
    fromProtobuf(toProtobuf(refundTxSignatureResponse)).refundSignature.encodeToBitcoin() should be (
      refundTxSignatureResponse.refundSignature.encodeToBitcoin())
    fromProtobuf(toProtobuf(refundTxSignatureResponse)).exchangeId should be (
      refundTxSignatureResponse.exchangeId)
  }

  "An Enter Exchange" should "be converted to protobuf and back again" in {
    val enterExchange = EnterExchange(
    exchangeId = "exchangeId",
    commitmentTransaction = new Transaction(new NetworkParameters {})
    )
    fromProtobuf(toProtobuf(enterExchange)) should be (enterExchange)
  }
}
