package com.coinffeine.common.protocol.serialization

import java.math.BigInteger

import com.google.bitcoin.params.UnitTestParams
import com.google.protobuf.{ByteString, Message}

import com.coinffeine.common._
import com.coinffeine.common.Currency.Euro
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin._
import com.coinffeine.common.exchange.{Both, Exchange}
import com.coinffeine.common.network.CoinffeineUnitTestNetwork
import com.coinffeine.common.protocol.messages.arbitration.CommitmentNotification
import com.coinffeine.common.protocol.messages.brokerage._
import com.coinffeine.common.protocol.messages.handshake._
import com.coinffeine.common.protocol.protobuf.{CoinffeineProtobuf => msg}

class DefaultProtoMappingsTest extends UnitTest with CoinffeineUnitTestNetwork.Component {

  val commitmentTransaction = ImmutableTransaction(new MutableTransaction(network))
  val txSerialization = new TransactionSerialization(network)
  val testMappings = new DefaultProtoMappings(txSerialization)
  import testMappings._

  def thereIsAMappingBetween[T, M <: Message](obj: T, msg: M)
                                             (implicit mapping: ProtoMapping[T, M]): Unit = {

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

  val sampleTxId = new Hash("d03f71f44d97243a83804b227cee881280556e9e73e5110ecdcb1bbf72d75c71")
  val sampleExchangeId = Exchange.Id.random()

  val btcAmount = 1.1 BTC
  val btcAmountMessage = msg.BtcAmount.newBuilder
    .setValue(11)
    .setScale(1)
    .build()
  "BTC amount" should behave like thereIsAMappingBetween(btcAmount, btcAmountMessage)

  "Fiat amount" should behave like thereIsAMappingBetween(3 EUR: FiatAmount, msg.FiatAmount.newBuilder
    .setCurrency("EUR")
    .setScale(0)
    .setValue(3)
    .build
  )

  val orderSetMessage = msg.OrderSet.newBuilder
    .setMarket(msg.Market.newBuilder.setCurrency("EUR"))
    .addBids(msg.Order.newBuilder
      .setAmount(msg.BtcAmount.newBuilder.setValue(1).setScale(0))
      .setPrice(msg.FiatAmount.newBuilder.setValue(400).setScale(0).setCurrency("EUR"))
    ).addAsks(msg.Order.newBuilder
      .setAmount(msg.BtcAmount.newBuilder.setValue(2).setScale(0))
      .setPrice(msg.FiatAmount.newBuilder.setValue(500).setScale(0).setCurrency("EUR"))
    ).build
  val orderSet = OrderSet(
    market = Market(Currency.Euro),
    bids = VolumeByPrice(400.EUR -> 1.BTC),
    asks = VolumeByPrice(500.EUR -> 2.BTC)
  )

  "OrderSet" should behave like thereIsAMappingBetween[OrderSet[FiatCurrency], msg.OrderSet](
    orderSet, orderSetMessage)

  val commitmentNotification = CommitmentNotification(sampleExchangeId, Both(sampleTxId, sampleTxId))
  val commitmentNotificationMessage = msg.CommitmentNotification.newBuilder()
    .setExchangeId(sampleExchangeId.value)
    .setBuyerTxId(ByteString.copyFrom(sampleTxId.getBytes))
    .setSellerTxId(ByteString.copyFrom(sampleTxId.getBytes))
    .build()

  "Commitment notification" should behave like thereIsAMappingBetween(
    commitmentNotification, commitmentNotificationMessage)

  val commitment = ExchangeCommitment(sampleExchangeId, commitmentTransaction)
  val commitmentMessage = msg.ExchangeCommitment.newBuilder()
    .setExchangeId(sampleExchangeId.value)
    .setCommitmentTransaction( txSerialization.serialize(commitmentTransaction))
    .build()

  "Enter exchange" must behave like thereIsAMappingBetween(commitment, commitmentMessage)

  val exchangeAborted = ExchangeAborted(Exchange.Id(sampleExchangeId.value), "a reason")
  val exchangeAbortedMessage = msg.ExchangeAborted.newBuilder()
    .setExchangeId(sampleExchangeId.value)
    .setReason("a reason")
    .build()

  "Exchange aborted" should behave like thereIsAMappingBetween(
    exchangeAborted, exchangeAbortedMessage)

  val exchangeRejection = ExchangeRejection(
    exchangeId = sampleExchangeId,
    reason = "a reason")
  val exchangeRejectionMessage = msg.ExchangeRejection.newBuilder()
    .setExchangeId(sampleExchangeId.value)
    .setReason("a reason")
    .build()

  "Exchange rejection" should behave like thereIsAMappingBetween(
    exchangeRejection, exchangeRejectionMessage)

  val orderMatch = OrderMatch(
    exchangeId = sampleExchangeId,
    amount = 0.1 BTC,
    price = 10000 EUR,
    participants = Both(
      buyer = PeerConnection("buyer", 8080),
      seller = PeerConnection("seller", 1234)
    )
  )
  val orderMatchMessage = msg.OrderMatch.newBuilder
    .setExchangeId(sampleExchangeId.value)
    .setAmount(ProtoMapping.toProtobuf[BitcoinAmount, msg.BtcAmount](0.1 BTC))
    .setPrice(ProtoMapping.toProtobuf[FiatAmount, msg.FiatAmount](10000 EUR))
    .setBuyer("coinffeine://buyer:8080/")
    .setSeller("coinffeine://seller:1234/")
    .build
  "Order match" must behave like thereIsAMappingBetween(orderMatch, orderMatchMessage)

  val emptyQuoteMessage = msg.Quote.newBuilder.setCurrency(Euro.javaCurrency.getCurrencyCode).build
  val emptyQuote = Quote.empty(Euro)
  "Empty quote" must behave like thereIsAMappingBetween[Quote[FiatCurrency], msg.Quote](
    emptyQuote, emptyQuoteMessage)

  val quoteMessage = emptyQuoteMessage.toBuilder
    .setHighestBid(ProtoMapping.toProtobuf[FiatAmount, msg.FiatAmount](20 EUR))
    .setLowestAsk(ProtoMapping.toProtobuf[FiatAmount, msg.FiatAmount](30 EUR))
    .setLastPrice(ProtoMapping.toProtobuf[FiatAmount, msg.FiatAmount](22 EUR))
    .build
  val quote = Quote(20.EUR -> 30.EUR, 22 EUR)
  "Quote" must behave like thereIsAMappingBetween[Quote[FiatCurrency], msg.Quote](
    quote, quoteMessage)

  val quoteRequest = QuoteRequest(Euro)
  val quoteRequestMessage = msg.QuoteRequest.newBuilder
    .setCurrency("EUR")
    .build

  "Quote request" must behave like thereIsAMappingBetween(quoteRequest, quoteRequestMessage)

  val refundTx = ImmutableTransaction(new MutableTransaction(UnitTestParams.get()))
  val peerHandshake = PeerHandshake(sampleExchangeId, refundTx, "accountId")
  val peerHandshakeMessage = msg.PeerHandshake.newBuilder()
    .setExchangeId(sampleExchangeId.value)
    .setRefundTx(ByteString.copyFrom(refundTx.get.bitcoinSerialize()))
    .setPaymentProcessorAccount("accountId")
    .build()

  "Peer handshake" must behave like thereIsAMappingBetween(peerHandshake, peerHandshakeMessage)

  val refundTxSignature = new TransactionSignature(BigInteger.ZERO, BigInteger.ZERO)
  val peerHandshakeResponse = PeerHandshakeAccepted(sampleExchangeId, refundTxSignature)
  val peerHandshakeResponseMessage = msg.PeerHandshakeAccepted.newBuilder()
    .setExchangeId(sampleExchangeId.value)
    .setTransactionSignature(ByteString.copyFrom(refundTxSignature.encodeToBitcoin()))
    .build()

  "Peer handshake response" must behave like thereIsAMappingBetween(
    peerHandshakeResponse, peerHandshakeResponseMessage)
}
