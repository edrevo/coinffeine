package com.bitwise.bitmarket.common.protocol.protobuf;

import java.math.BigDecimal;
import java.util.Currency;

import com.bitwise.bitmarket.common.PeerConnectionParser;
import com.bitwise.bitmarket.common.currency.Amount;
import com.bitwise.bitmarket.common.protocol.Offer;
import com.bitwise.bitmarket.common.protocol.OfferId;
import com.bitwise.bitmarket.common.protocol.OfferType;
import com.bitwise.bitmarket.common.protocol.PeerId;

public class OfferConversions {

    public static Offer fromProtobuf(BitmarketProtobuf.PublishOffer proto) {
        return new Offer(
                new OfferId(proto.getId()),
                proto.getSeq(),
                fromProtobuf(proto.getType()),
                new PeerId(proto.getFrom()),
                PeerConnectionParser.parse(proto.getConnection()),
                fromProtobuf(proto.getAmount()));
    }

    public static OfferType fromProtobuf(BitmarketProtobuf.OfferType offerType) {
        switch (offerType.getNumber()) {
            case BitmarketProtobuf.OfferType.BUY_VALUE:
                return OfferType.BUY_OFFER;
            case BitmarketProtobuf.OfferType.SELL_VALUE:
                return OfferType.SELL_OFFER;
            default:
                throw new IllegalArgumentException(String.format(
                        "no conversion defined of offer type %s from protobuf to internal representation",
                        offerType.toString()));
        }
    }

    public static Amount fromProtobuf(BitmarketProtobuf.Amount amount) {
        return new Amount(
                BigDecimal.valueOf(amount.getValue(), amount.getScale()),
                Currency.getInstance(amount.getCurrency()));
    }

    public static BitmarketProtobuf.PublishOffer toProtobuf(Offer offer) {
        return BitmarketProtobuf.PublishOffer.newBuilder()
                .setId(offer.getId().getBytes())
                .setSeq(offer.getSequenceNumber())
                .setType(toProtobuf(offer.getOfferType()))
                .setFrom(offer.getFromId().getAddress())
                .setConnection(offer.getFromConnection().toString())
                .setAmount(toProtobuf(offer.getAmount()))
                .build();
    }

    public static BitmarketProtobuf.OfferType toProtobuf(OfferType offerType) {
        switch (offerType) {
            case BUY_OFFER: return BitmarketProtobuf.OfferType.BUY;
            case SELL_OFFER: return BitmarketProtobuf.OfferType.SELL;
            default:
                throw new IllegalArgumentException(String.format(
                        "no conversion defined of offer type %s from internal representation to protobuf",
                        offerType.toString()));
        }
    }

    public static BitmarketProtobuf.Amount toProtobuf(Amount amount) {
        return BitmarketProtobuf.Amount.newBuilder()
                .setValue(amount.getAmount().unscaledValue().longValue())
                .setScale(amount.getAmount().scale())
                .setCurrency(amount.getCurrency().getSymbol())
                .build();
    }

    private OfferConversions() {}
}
