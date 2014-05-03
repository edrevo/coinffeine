package com.coinffeine.common.protocol.serialization

import com.google.protobuf.Descriptors.FieldDescriptor

import com.coinffeine.common.protocol.Version
import com.coinffeine.common.protocol.messages.PublicMessage
import com.coinffeine.common.protocol.messages.arbitration.CommitmentNotification
import com.coinffeine.common.protocol.messages.brokerage._
import com.coinffeine.common.protocol.messages.exchange._
import com.coinffeine.common.protocol.messages.handshake._
import com.coinffeine.common.protocol.protobuf.{CoinffeineProtobuf => proto}
import com.coinffeine.common.protocol.protobuf.CoinffeineProtobuf.ProtocolVersion
import com.coinffeine.common.protocol.protobuf.CoinffeineProtobuf.Payload._

private[serialization] class DefaultProtocolSerialization(
    version: Version,
    transactionSerialization: TransactionSerialization) extends ProtocolSerialization {

  private val protoVersion = proto.ProtocolVersion.newBuilder()
    .setMajor(version.major)
    .setMinor(version.minor)
    .build()
  private val mappings = new DefaultProtoMappings(transactionSerialization)
  import mappings._

  override def toProtobuf(message: PublicMessage): proto.CoinffeineMessage =
    proto.CoinffeineMessage.newBuilder()
      .setVersion(protoVersion)
      .setPayload(toPayload(message)).build()

  private def toPayload(message: PublicMessage): proto.Payload.Builder = {
    val builder = proto.Payload.newBuilder
    message match {
      case m: ExchangeAborted =>
        builder.setExchangeAborted(ProtoMapping.toProtobuf(m))
      case m: EnterExchange =>
        builder.setEnterExchange(ProtoMapping.toProtobuf(m))
      case m: CommitmentNotification =>
        builder.setCommitmentNotification(ProtoMapping.toProtobuf(m))
      case m: OrderMatch =>
        builder.setOrderMatch(ProtoMapping.toProtobuf(m))
      case m: Order =>
        builder.setOrder(ProtoMapping.toProtobuf(m))
      case m: CancelOrder =>
        builder.setCancelOrder(ProtoMapping.toProtobuf(m))
      case m: QuoteRequest =>
        builder.setQuoteRequest(ProtoMapping.toProtobuf(m))
      case m: Quote =>
        builder.setQuote(ProtoMapping.toProtobuf(m))
      case m: ExchangeRejection =>
        builder.setExchangeRejection(ProtoMapping.toProtobuf(m))
      case m: RefundTxSignatureRequest =>
        builder.setRefundTxSignatureRequest(ProtoMapping.toProtobuf(m))
      case m: RefundTxSignatureResponse =>
        builder.setRefundTxSignatureResponse(ProtoMapping.toProtobuf(m))
      case m: StepSignature =>
        builder.setStepSignature(ProtoMapping.toProtobuf(m))
      case m: PaymentProof =>
        builder.setPaymentProof(ProtoMapping.toProtobuf(m))
      case _ => throw new IllegalArgumentException("Unsupported message: " + message)
    }
    builder
  }

  override def fromProtobuf(message: proto.CoinffeineMessage): PublicMessage = {
    requireSameVersion(message.getVersion)
    fromPayload(message.getPayload)
  }

  private def requireSameVersion(messageVersion: ProtocolVersion): Unit = {
    val parsedVersion = Version(messageVersion.getMajor, messageVersion.getMinor)
    require(version == parsedVersion,
      s"Cannot deserialize message with version $parsedVersion, expected version $version")
  }

  private def fromPayload(payload: proto.Payload): PublicMessage = {
    val messageFields = payload.getAllFields
    val fieldNumber: Int = messageFields.size()
    require(fieldNumber >= 1, "Message has no content")
    require(fieldNumber <= 1, s"Malformed message with $fieldNumber fields: $payload")
    val descriptor: FieldDescriptor = messageFields.keySet().iterator().next()
    descriptor.getNumber match {
      case EXCHANGEABORTED_FIELD_NUMBER =>
        ProtoMapping.fromProtobuf(payload.getExchangeAborted)
      case ENTEREXCHANGE_FIELD_NUMBER =>
        ProtoMapping.fromProtobuf(payload.getEnterExchange)
      case COMMITMENTNOTIFICATION_FIELD_NUMBER =>
        ProtoMapping.fromProtobuf(payload.getCommitmentNotification)
      case ORDERMATCH_FIELD_NUMBER =>
        ProtoMapping.fromProtobuf(payload.getOrderMatch)
      case ORDER_FIELD_NUMBER =>
        ProtoMapping.fromProtobuf(payload.getOrder)
      case CANCELORDER_FIELD_NUMBER =>
        ProtoMapping.fromProtobuf(payload.getCancelOrder)
      case QUOTEREQUEST_FIELD_NUMBER =>
        ProtoMapping.fromProtobuf(payload.getQuoteRequest)
      case QUOTE_FIELD_NUMBER =>
        ProtoMapping.fromProtobuf(payload.getQuote)
      case EXCHANGEREJECTION_FIELD_NUMBER =>
        ProtoMapping.fromProtobuf(payload.getExchangeRejection)
      case REFUNDTXSIGNATUREREQUEST_FIELD_NUMBER =>
        ProtoMapping.fromProtobuf(payload.getRefundTxSignatureRequest)
      case REFUNDTXSIGNATURERESPONSE_FIELD_NUMBER =>
        ProtoMapping.fromProtobuf(payload.getRefundTxSignatureResponse)
      case STEPSIGNATURE_FIELD_NUMBER =>
        ProtoMapping.fromProtobuf(payload.getStepSignature)
      case PAYMENTPROOF_FIELD_NUMBER =>
        ProtoMapping.fromProtobuf(payload.getPaymentProof)
      case _ => throw new IllegalArgumentException("Unsupported message: " + descriptor.getFullName)
    }
  }
}
