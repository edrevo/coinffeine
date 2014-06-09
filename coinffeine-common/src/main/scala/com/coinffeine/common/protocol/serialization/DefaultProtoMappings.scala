package com.coinffeine.common.protocol.serialization

import java.math.BigDecimal
import java.util.Currency
import scala.collection.JavaConverters._

import com.google.bitcoin.core.Sha256Hash
import com.google.protobuf.ByteString

import com.coinffeine.common.{CurrencyAmount, FiatCurrency, PeerConnection, BitcoinAmount}
import com.coinffeine.common.Currency.Bitcoin
import com.coinffeine.common.protocol.messages.arbitration.CommitmentNotification
import com.coinffeine.common.protocol.messages.brokerage._
import com.coinffeine.common.protocol.messages.exchange._
import com.coinffeine.common.protocol.messages.handshake._
import com.coinffeine.common.protocol.protobuf.{CoinffeineProtobuf => msg}

/** Implicit conversion mappings for the protocol messages */
private[serialization] class DefaultProtoMappings(txSerialization: TransactionSerialization) {

  implicit val commitmentNotificationMapping =
    new ProtoMapping[CommitmentNotification, msg.CommitmentNotification] {

      override def fromProtobuf(commitment: msg.CommitmentNotification) = CommitmentNotification(
        exchangeId = commitment.getExchangeId,
        buyerTxId = new Sha256Hash(commitment.getBuyerTxId.toByteArray),
        sellerTxId = new Sha256Hash(commitment.getSellerTxId.toByteArray)
      )

      override def toProtobuf(commitment: CommitmentNotification) =
        msg.CommitmentNotification.newBuilder
          .setExchangeId(commitment.exchangeId)
          .setBuyerTxId(ByteString.copyFrom(commitment.buyerTxId.getBytes))
          .setSellerTxId(ByteString.copyFrom(commitment.sellerTxId.getBytes))
          .build
    }

  implicit val enterExchangeMapping = new ProtoMapping[ExchangeCommitment, msg.ExchangeCommitment] {

      override def fromProtobuf(enter: msg.ExchangeCommitment) = ExchangeCommitment(
        commitmentTransaction = txSerialization.deserializeTransaction(
          enter.getCommitmentTransaction),
        exchangeId = enter.getExchangeId
      )

      override def toProtobuf(enter: ExchangeCommitment) = msg.ExchangeCommitment.newBuilder
        .setExchangeId(enter.exchangeId)
        .setCommitmentTransaction(txSerialization.serialize(enter.commitmentTransaction)).build
    }

  implicit val exchangeAbortedMapping = new ProtoMapping[ExchangeAborted, msg.ExchangeAborted] {

    override def fromProtobuf(exchangeAborted: msg.ExchangeAborted) = ExchangeAborted(
      exchangeId = exchangeAborted.getExchangeId,
      reason = exchangeAborted.getReason
    )

    override def toProtobuf(exchangeAborted: ExchangeAborted) = msg.ExchangeAborted.newBuilder
      .setExchangeId(exchangeAborted.exchangeId)
      .setReason(exchangeAborted.reason)
      .build
  }

  implicit val exchangeRejectionMapping = new ProtoMapping[ExchangeRejection, msg.ExchangeRejection] {

    override def fromProtobuf(rejection: msg.ExchangeRejection) = ExchangeRejection(
      exchangeId = rejection.getExchangeId,
      reason = rejection.getReason
    )

    override def toProtobuf(rejection: ExchangeRejection) = msg.ExchangeRejection.newBuilder
      .setExchangeId(rejection.exchangeId)
      .setReason(rejection.reason)
      .build
  }

  implicit val fiatAmountMapping = new ProtoMapping[CurrencyAmount[FiatCurrency], msg.FiatAmount] {

    override def fromProtobuf(amount: msg.FiatAmount): CurrencyAmount[FiatCurrency] =
      FiatCurrency(Currency.getInstance(amount.getCurrency)).amount(
        BigDecimal.valueOf(amount.getValue, amount.getScale))

    override def toProtobuf(amount: CurrencyAmount[FiatCurrency]): msg.FiatAmount = msg.FiatAmount.newBuilder
      .setValue(amount.value.underlying().unscaledValue.longValue)
      .setScale(amount.value.scale)
      .setCurrency(amount.currency.javaCurrency.getCurrencyCode)
      .build
  }

  implicit val btcAmountMapping = new ProtoMapping[BitcoinAmount, msg.BtcAmount] {

    override def fromProtobuf(amount: msg.BtcAmount): BitcoinAmount =
      Bitcoin.amount(BigDecimal.valueOf(amount.getValue, amount.getScale))

    override def toProtobuf(amount: BitcoinAmount): msg.BtcAmount = msg.BtcAmount.newBuilder
      .setValue(amount.value.underlying().unscaledValue.longValue)
      .setScale(amount.value.scale)
      .build
  }

  implicit val marketMapping = new ProtoMapping[Market[FiatCurrency], msg.Market] {

    override def fromProtobuf(market: msg.Market): Market[FiatCurrency] =
      Market(FiatCurrency(Currency.getInstance(market.getCurrency)))

    override def toProtobuf(market: Market[FiatCurrency]): msg.Market = msg.Market.newBuilder
      .setCurrency(market.currency.javaCurrency.getCurrencyCode)
      .build
  }

  implicit val orderSetMapping = new ProtoMapping[OrderSet[FiatCurrency], msg.OrderSet] {

    override def fromProtobuf(orderSet: msg.OrderSet): OrderSet[FiatCurrency] = {
      val market = ProtoMapping.fromProtobuf(orderSet.getMarket)

      def volumeFromProtobuf(entries: Seq[msg.Order]): VolumeByPrice[FiatCurrency] = {
        val accum = VolumeByPrice.empty[FiatCurrency]
        entries.foldLeft(accum) { (volume, entry) => volume.increase(
            ProtoMapping.fromProtobuf(entry.getPrice),
            ProtoMapping.fromProtobuf(entry.getAmount)
        )}
      }

      OrderSet(
        market,
        bids = volumeFromProtobuf(orderSet.getBidsList.asScala),
        asks = volumeFromProtobuf(orderSet.getAsksList.asScala)
      )
    }

    override def toProtobuf(orderSet: OrderSet[FiatCurrency]): msg.OrderSet = {
      msg.OrderSet.newBuilder
        .setMarket(ProtoMapping.toProtobuf(orderSet.market))
        .addAllBids(volumeToProtobuf(orderSet.bids).asJava)
        .addAllAsks(volumeToProtobuf(orderSet.asks).asJava)
        .build
    }

    private def volumeToProtobuf(volume: VolumeByPrice[FiatCurrency]) = for {
      (price, amount) <- volume.entries
    } yield msg.Order.newBuilder
        .setPrice(ProtoMapping.toProtobuf(price))
        .setAmount(ProtoMapping.toProtobuf(amount))
        .build
  }

  implicit val orderMatchMapping = new ProtoMapping[OrderMatch, msg.OrderMatch] {

    override def fromProtobuf(orderMatch: msg.OrderMatch): OrderMatch = OrderMatch(
      exchangeId = orderMatch.getExchangeId,
      amount = ProtoMapping.fromProtobuf(orderMatch.getAmount),
      price = ProtoMapping.fromProtobuf(orderMatch.getPrice),
      buyer = PeerConnection.parse(orderMatch.getBuyer),
      seller = PeerConnection.parse(orderMatch.getSeller)
    )

    override def toProtobuf(orderMatch: OrderMatch): msg.OrderMatch = msg.OrderMatch.newBuilder
      .setExchangeId(orderMatch.exchangeId)
      .setAmount(ProtoMapping.toProtobuf(orderMatch.amount))
      .setPrice(ProtoMapping.toProtobuf(orderMatch.price))
      .setBuyer(orderMatch.buyer.toString)
      .setSeller(orderMatch.seller.toString)
      .build
  }

  implicit val quoteMapping = new ProtoMapping[Quote[FiatCurrency], msg.Quote] {

    override def fromProtobuf(quote: msg.Quote): Quote[FiatCurrency] = {
      val bidOption =
        if (quote.hasHighestBid) Some(ProtoMapping.fromProtobuf(quote.getHighestBid)) else None
      val askOption =
        if (quote.hasLowestAsk) Some(ProtoMapping.fromProtobuf(quote.getLowestAsk)) else None
      val lastPriceOption =
        if (quote.hasLastPrice) Some(ProtoMapping.fromProtobuf(quote.getLastPrice)) else None
      val currency = FiatCurrency(Currency.getInstance(quote.getCurrency))
      def requireCorrectCurrency(amount: Option[CurrencyAmount[FiatCurrency]]): Unit = {
        require(amount.forall(_.currency == currency),
          s"Incorrect currency. Expected $currency, received ${amount.get.currency}")
      }
      requireCorrectCurrency(bidOption)
      requireCorrectCurrency(askOption)
      requireCorrectCurrency(lastPriceOption)
      Quote(currency, bidOption -> askOption, lastPriceOption)
    }

    override def toProtobuf(quote: Quote[FiatCurrency]): msg.Quote = {
      val Quote(currency, (bidOption, askOption), lastPriceOption) = quote
      val builder = msg.Quote.newBuilder
        .setCurrency(currency.javaCurrency.getCurrencyCode)
      bidOption.foreach(bid => builder.setHighestBid(ProtoMapping.toProtobuf(bid)))
      askOption.foreach(ask => builder.setLowestAsk(ProtoMapping.toProtobuf(ask)))
      lastPriceOption.foreach(lastPrice => builder.setLastPrice(ProtoMapping.toProtobuf(lastPrice)))
      builder.build
    }
  }

  implicit val quoteRequestMapping = new ProtoMapping[QuoteRequest, msg.QuoteRequest] {

    override def fromProtobuf(request: msg.QuoteRequest): QuoteRequest =
      QuoteRequest(FiatCurrency(Currency.getInstance(request.getCurrency)))

    override def toProtobuf(request: QuoteRequest): msg.QuoteRequest = msg.QuoteRequest.newBuilder
      .setCurrency(request.currency.javaCurrency.getCurrencyCode)
      .build
  }

  implicit val refundTxSignatureRequestMapping =
    new ProtoMapping[RefundTxSignatureRequest, msg.RefundTxSignatureRequest] {

      override def fromProtobuf(request: msg.RefundTxSignatureRequest) = RefundTxSignatureRequest(
        refundTx = txSerialization.deserializeTransaction(request.getRefundTx),
        exchangeId = request.getExchangeId
      )

      override def toProtobuf(request: RefundTxSignatureRequest) =
        msg.RefundTxSignatureRequest.newBuilder
          .setExchangeId(request.exchangeId)
          .setRefundTx(txSerialization.serialize(request.refundTx))
          .build
    }

  implicit val refundTxSignatureResponseMapping =
    new ProtoMapping[RefundTxSignatureResponse, msg.RefundTxSignatureResponse] {

      override def fromProtobuf(response: msg.RefundTxSignatureResponse) = RefundTxSignatureResponse(
        exchangeId = response.getExchangeId,
        refundSignature = txSerialization.deserializeSignature(response.getTransactionSignature)
      )

      override def toProtobuf(response: RefundTxSignatureResponse) =
        msg.RefundTxSignatureResponse.newBuilder
          .setExchangeId(response.exchangeId)
          .setTransactionSignature(txSerialization.serialize(response.refundSignature))
          .build()
    }

  implicit val offerSignatureMapping = new ProtoMapping[StepSignatures, msg.StepSignature] {

    override def fromProtobuf(message: msg.StepSignature) = StepSignatures(
      exchangeId = message.getExchangeId,
      idx0Signature = txSerialization.deserializeSignature(message.getIdx0Signature),
      idx1Signature = txSerialization.deserializeSignature(message.getIdx1Signature)
    )

    override def toProtobuf(obj: StepSignatures) = msg.StepSignature.newBuilder
      .setExchangeId(obj.exchangeId)
      .setIdx0Signature(txSerialization.serialize(obj.idx0Signature))
      .setIdx1Signature(txSerialization.serialize(obj.idx1Signature))
      .build()
  }

  implicit val paymentProofMapping = new ProtoMapping[PaymentProof, msg.PaymentProof] {

    override def fromProtobuf(message: msg.PaymentProof) = PaymentProof(
      exchangeId = message.getExchangeId,
      paymentId = message.getPaymentId
    )

    override def toProtobuf(obj: PaymentProof): msg.PaymentProof = msg.PaymentProof.newBuilder
      .setExchangeId(obj.exchangeId)
      .setPaymentId(obj.paymentId)
      .build()
  }
}
