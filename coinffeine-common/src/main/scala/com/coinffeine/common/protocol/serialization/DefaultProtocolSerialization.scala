package com.coinffeine.common.protocol.serialization

import com.google.protobuf.Descriptors.FieldDescriptor

import com.coinffeine.common.protocol.messages.PublicMessage
import com.coinffeine.common.protocol.messages.arbitration.CommitmentNotification
import com.coinffeine.common.protocol.messages.handshake._
import com.coinffeine.common.protocol.messages.exchange._
import com.coinffeine.common.protocol.messages.brokerage._
import com.coinffeine.common.protocol.protobuf.{CoinffeineProtobuf => proto}
import com.coinffeine.common.protocol.protobuf.CoinffeineProtobuf.CoinffeineMessage._

private[serialization] class DefaultProtocolSerialization(
    transactionSerialization: TransactionSerialization) extends ProtocolSerialization {

  val mappings = new DefaultProtoMappings(transactionSerialization)
  import mappings._

  override def toProtobuf(message: PublicMessage): proto.CoinffeineMessage = {
    val builder = proto.CoinffeineMessage.newBuilder
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
    builder.build()
  }

  override def fromProtobuf(message: proto.CoinffeineMessage): PublicMessage = {
    val messageFields = message.getAllFields
    val fieldNumber: Int = messageFields.size()
    require(fieldNumber >= 1, "Message has no content")
    require(fieldNumber <= 1, s"Malformed message with $fieldNumber fields: $message")
    val descriptor: FieldDescriptor = messageFields.keySet().iterator().next()
    descriptor.getNumber match {
      case EXCHANGEABORTED_FIELD_NUMBER =>
        ProtoMapping.fromProtobuf(message.getExchangeAborted)
      case ENTEREXCHANGE_FIELD_NUMBER =>
        ProtoMapping.fromProtobuf(message.getEnterExchange)
      case COMMITMENTNOTIFICATION_FIELD_NUMBER =>
        ProtoMapping.fromProtobuf(message.getCommitmentNotification)
      case ORDERMATCH_FIELD_NUMBER =>
        ProtoMapping.fromProtobuf(message.getOrderMatch)
      case ORDER_FIELD_NUMBER =>
        ProtoMapping.fromProtobuf(message.getOrder)
      case CANCELORDER_FIELD_NUMBER =>
        ProtoMapping.fromProtobuf(message.getCancelOrder)
      case QUOTEREQUEST_FIELD_NUMBER =>
        ProtoMapping.fromProtobuf(message.getQuoteRequest)
      case QUOTE_FIELD_NUMBER =>
        ProtoMapping.fromProtobuf(message.getQuote)
      case EXCHANGEREJECTION_FIELD_NUMBER =>
        ProtoMapping.fromProtobuf(message.getExchangeRejection)
      case REFUNDTXSIGNATUREREQUEST_FIELD_NUMBER =>
        ProtoMapping.fromProtobuf(message.getRefundTxSignatureRequest)
      case REFUNDTXSIGNATURERESPONSE_FIELD_NUMBER =>
        ProtoMapping.fromProtobuf(message.getRefundTxSignatureResponse)
      case STEPSIGNATURE_FIELD_NUMBER =>
        ProtoMapping.fromProtobuf(message.getStepSignature)
      case PAYMENTPROOF_FIELD_NUMBER =>
        ProtoMapping.fromProtobuf(message.getPaymentProof)
      case _ => throw new IllegalArgumentException("Unsupported message: " + descriptor.getFullName)
    }
  }
}
