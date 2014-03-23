package com.coinffeine.common.protocol.serialization

import java.math.BigInteger.ZERO
import scala.collection.JavaConversions

import com.google.bitcoin.core.{Sha256Hash, Transaction}
import com.google.bitcoin.crypto.TransactionSignature
import com.google.bitcoin.params.UnitTestParams
import org.reflections.Reflections
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.currency.{CurrencyCode, BtcAmount}
import com.coinffeine.common.protocol.messages.PublicMessage
import com.coinffeine.common.protocol.messages.arbitration.CommitmentNotification
import com.coinffeine.common.protocol.messages.brokerage._
import com.coinffeine.common.protocol.messages.exchange._
import com.coinffeine.common.protocol.messages.handshake._
import com.coinffeine.common.protocol.protobuf.{CoinffeineProtobuf => proto}
import com.coinffeine.common.protocol.protobuf.CoinffeineProtobuf.CoinffeineMessage

class DefaultProtocolSerializationTest extends FlatSpec with ShouldMatchers {

  val exchangeId = "exchangeid"
  val serializedTransaction = new Transaction(UnitTestParams.get()).bitcoinSerialize()
  val sampleTxId = new Sha256Hash("d03f71f44d97243a83804b227cee881280556e9e73e5110ecdcb1bbf72d75c71")
  val btcAmount = BtcAmount(1)
  val fiatAmount = CurrencyCode.EUR(1)
  val peerConnection = PeerConnection("host", 8888)
  val sampleMessages = Seq(
    ExchangeAborted(exchangeId, "reason"),
    EnterExchange(exchangeId, serializedTransaction),
    CommitmentNotification(exchangeId, sampleTxId, sampleTxId),
    OrderMatch(exchangeId, btcAmount, fiatAmount, peerConnection, peerConnection),
    OrderCancellation(CurrencyCode.USD.currency),
    Order(Bid, btcAmount, fiatAmount),
    QuoteRequest(CurrencyCode.USD.currency),
    Quote(fiatAmount -> fiatAmount, fiatAmount),
    ExchangeRejection(exchangeId, "reason"),
    RefundTxSignatureRequest(exchangeId, serializedTransaction),
    RefundTxSignatureResponse(exchangeId, new TransactionSignature(ZERO, ZERO)),
  )

  requireSampleInstancesForAllPublicMessages()

  "The default protocol serialization" should
    "support roundtrip serialization for all public messages" in {
    sampleMessages.foreach { originalMessage =>
      val protoMessage = DefaultProtocolSerialization.toProtobuf(originalMessage)
      DefaultProtocolSerialization.fromProtobuf(protoMessage) should be (originalMessage)
    }
  }

  it must "throw when serializing unknown public messages" in {
    val ex = evaluating {
      DefaultProtocolSerialization.toProtobuf(new PublicMessage {})
    } should produce [IllegalArgumentException]
    ex.getMessage should include ("Unsupported message")
  }

  it must "throw when deserializing an empty protobuf message" in {
    val emptyMessage = CoinffeineMessage.newBuilder.build
    val ex = evaluating {
      DefaultProtocolSerialization.fromProtobuf(emptyMessage)
    } should produce [IllegalArgumentException]
    ex.getMessage should include ("Message has no content")
  }

  it must "throw when deserializing a protobuf message with multiple messages" in {
    val emptyMessage = CoinffeineMessage.newBuilder
      .setExchangeAborted(proto.ExchangeAborted.newBuilder.setExchangeId("id").setReason("reason"))
      .setQuoteRequest(proto.QuoteRequest.newBuilder.setCurrency("USD"))
      .build
    val ex = evaluating {
      DefaultProtocolSerialization.fromProtobuf(emptyMessage)
    } should produce [IllegalArgumentException]
    ex.getMessage should include ("Malformed message with 2 fields")
  }

  /** Make sure we have a working serialization for all defined public messages. */
  private def requireSampleInstancesForAllPublicMessages(): Unit = {
    scanPublicMessagesFromClasspath().foreach { messageClass =>
      require(
        sampleMessages.exists(_.getClass == messageClass),
        s"There is not a sample instance of ${messageClass.getCanonicalName}"
      )
    }
  }

  private def scanPublicMessagesFromClasspath(): Set[Class[_ <: PublicMessage]] = {
    val publicMessage = classOf[PublicMessage]
    val basePackage = publicMessage.getPackage.getName
    val reflections = new Reflections(basePackage)
    JavaConversions.asScalaSet(reflections.getSubTypesOf(publicMessage)).toSet
  }
}
