package com.bitwise.bitmarket.common.protocol.protobuf;

import java.math.BigDecimal;
import java.util.Currency;

import com.bitwise.bitmarket.common.PeerConnectionParser;
import com.bitwise.bitmarket.common.currency.BtcAmount;
import com.bitwise.bitmarket.common.currency.FiatAmount;
import com.bitwise.bitmarket.common.protocol.Offer;
import com.bitwise.bitmarket.common.protocol.OfferId;
import com.bitwise.bitmarket.common.protocol.PeerId;
import com.bitwise.bitmarket.common.protocol.protobuf.BitmarketProtobuf.ExchangeRequest;

public class ProtobufConversions {

    public static Offer fromProtobuf(BitmarketProtobuf.Offer proto) {
        return new Offer(
                new OfferId(proto.getId()),
                proto.getSeq(),
                new PeerId(proto.getFrom()),
                PeerConnectionParser.parse(proto.getConnection()),
                fromProtobuf(proto.getAmount()),
                fromProtobuf(proto.getBtcPrice()));
    }

    public static com.bitwise.bitmarket.common.protocol.ExchangeRequest fromProtobuf(ExchangeRequest proto) {
        return new com.bitwise.bitmarket.common.protocol.ExchangeRequest(
                new OfferId(proto.getId()),
                new PeerId(proto.getFrom()),
                PeerConnectionParser.parse(proto.getConnection()),
                fromProtobuf(proto.getAmount()));
    }

    public static BtcAmount fromProtobuf(BitmarketProtobuf.BtcAmount amount) {
        return new BtcAmount(
                BigDecimal.valueOf(amount.getValue(), amount.getScale()));
    }

    public static FiatAmount fromProtobuf(BitmarketProtobuf.FiatAmount amount) {
        return new FiatAmount(
                BigDecimal.valueOf(amount.getValue(), amount.getScale()),
                Currency.getInstance(amount.getCurrency()));
    }

    public static BitmarketProtobuf.Offer toProtobuf(Offer offer) {
        return BitmarketProtobuf.Offer.newBuilder()
                .setId(offer.getId().getBytes())
                .setSeq(offer.getSequenceNumber())
                .setFrom(offer.getFromId().getAddress())
                .setConnection(offer.getFromConnection().toString())
                .setAmount(toProtobuf(offer.getAmount()))
                .setBtcPrice(toProtobuf(offer.getBitcoinPrice()))
                .build();
    }

    public static ExchangeRequest toProtobuf(
            com.bitwise.bitmarket.common.protocol.ExchangeRequest acceptance) {
        return BitmarketProtobuf.ExchangeRequest.newBuilder()
                .setId(acceptance.getId().getBytes())
                .setFrom(acceptance.getFromId().getAddress())
                .setConnection(acceptance.getFromConnection().toString())
                .setAmount(toProtobuf(acceptance.getAmount()))
                .build();
    }

    public static BitmarketProtobuf.BtcAmount toProtobuf(BtcAmount amount) {
        return BitmarketProtobuf.BtcAmount.newBuilder()
                .setValue(amount.amount().underlying().unscaledValue().longValue())
                .setScale(amount.amount().scale())
                .build();
    }

    public static BitmarketProtobuf.FiatAmount toProtobuf(FiatAmount amount) {
        return BitmarketProtobuf.FiatAmount.newBuilder()
                .setValue(amount.amount().underlying().unscaledValue().longValue())
                .setScale(amount.amount().scale())
                .setCurrency(amount.currency().getCurrencyCode())
                .build();
    }

    private ProtobufConversions() {}
}
