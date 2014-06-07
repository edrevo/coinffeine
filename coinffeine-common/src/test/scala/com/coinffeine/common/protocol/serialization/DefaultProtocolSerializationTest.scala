package com.coinffeine.common.protocol.serialization

import java.math.BigInteger.ZERO
import scala.collection.JavaConversions

import com.google.bitcoin.core.{Sha256Hash, Transaction}
import com.google.bitcoin.crypto.TransactionSignature
import org.reflections.Reflections

import com.coinffeine.common.{Currency, PeerConnection, UnitTest}
import com.coinffeine.common.currency.CurrencyCode
import com.coinffeine.common.currency.Implicits._
import com.coinffeine.common.network.UnitTestNetworkComponent
import com.coinffeine.common.protocol.Version
import com.coinffeine.common.protocol.messages.PublicMessage
import com.coinffeine.common.protocol.messages.arbitration.CommitmentNotification
import com.coinffeine.common.protocol.messages.brokerage._
import com.coinffeine.common.protocol.messages.exchange._
import com.coinffeine.common.protocol.messages.handshake._
import com.coinffeine.common.protocol.protobuf.{CoinffeineProtobuf => proto}
import com.coinffeine.common.protocol.protobuf.CoinffeineProtobuf.CoinffeineMessage

class DefaultProtocolSerializationTest extends UnitTest with UnitTestNetworkComponent {

  val exchangeId = "exchangeid"
  val transaction = new Transaction(network)
  val transactionSignature = new TransactionSignature(ZERO, ZERO)
  val sampleTxId = new Sha256Hash("d03f71f44d97243a83804b227cee881280556e9e73e5110ecdcb1bbf72d75c71")
  val btcAmount = 1.BTC
  val fiatAmount = 1.EUR
  val peerConnection = PeerConnection("host", 8888)
  val sampleMessages = Seq(
    ExchangeAborted(exchangeId, "reason"),
    ExchangeCommitment(exchangeId, transaction),
    CommitmentNotification(exchangeId, sampleTxId, sampleTxId),
    OrderMatch(exchangeId, btcAmount, fiatAmount, peerConnection, peerConnection),
    OrderSet(
      market = Market(Currency.UsDollar),
      bids = VolumeByPrice(100.USD.toCurrencyAmount -> 1.3.BTC.toBitcoinAmount),
      asks = VolumeByPrice(200.USD.toCurrencyAmount -> 0.3.BTC.toBitcoinAmount,
        250.USD.toCurrencyAmount -> 0.4.BTC.toBitcoinAmount)
    ),
    QuoteRequest(CurrencyCode.USD.currency),
    Quote(fiatAmount -> fiatAmount, fiatAmount),
    ExchangeRejection(exchangeId, "reason"),
    RefundTxSignatureRequest(exchangeId, transaction),
    RefundTxSignatureResponse(exchangeId, transactionSignature),
    StepSignatures(exchangeId, transactionSignature, transactionSignature),
    PaymentProof(exchangeId, "paymentId")
  )
  val version = Version(major = 1, minor = 0)
  val protoVersion = proto.ProtocolVersion.newBuilder().setMajor(1).setMinor(0).build()
  val instance = new DefaultProtocolSerialization(version, new TransactionSerialization(network))

  requireSampleInstancesForAllPublicMessages()

  "The default protocol serialization" should
    "support roundtrip serialization for all public messages" in {
    sampleMessages.foreach { originalMessage =>
      val protoMessage = instance.toProtobuf(originalMessage)
      instance.fromProtobuf(protoMessage) should be (originalMessage)
    }
  }

  it must "throw when deserializing messages of a different protocol version" in {
    val message = CoinffeineMessage.newBuilder
      .setVersion(protoVersion.toBuilder.setMajor(42))
      .setPayload(
        proto.Payload.newBuilder.setQuoteRequest(proto.QuoteRequest.newBuilder.setCurrency("USD")))
      .build
    val ex = the [IllegalArgumentException] thrownBy {
      instance.fromProtobuf(message)
    }
    ex.getMessage should include ("Cannot deserialize message with version 42.0")
  }

  it must "throw when serializing unknown public messages" in {
    val ex = the [IllegalArgumentException] thrownBy {
      instance.toProtobuf(new PublicMessage {})
    }
    ex.getMessage should include ("Unsupported message")
  }

  it must "throw when deserializing an empty protobuf message" in {
    val emptyMessage = CoinffeineMessage.newBuilder
      .setVersion(protoVersion)
      .setPayload(proto.Payload.newBuilder())
      .build
    val ex = the [IllegalArgumentException] thrownBy {
      instance.fromProtobuf(emptyMessage)
    }
    ex.getMessage should include ("Message has no content")
  }

  it must "throw when deserializing a protobuf message with multiple messages" in {
    val multiMessage = CoinffeineMessage.newBuilder
      .setVersion(protoVersion)
      .setPayload(proto.Payload.newBuilder
        .setExchangeAborted(proto.ExchangeAborted.newBuilder.setExchangeId("id").setReason("reason"))
        .setQuoteRequest(proto.QuoteRequest.newBuilder.setCurrency("USD")))
      .build
    val ex = the [IllegalArgumentException] thrownBy {
      instance.fromProtobuf(multiMessage)
    }
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
