package com.bitwise.bitmarket.common.protocol.protobuf;

import java.util.Currency;

import com.bitwise.bitmarket.common.PeerConnectionParser;
import com.bitwise.bitmarket.common.currency.Amount;
import com.bitwise.bitmarket.common.protocol.Offer;
import com.bitwise.bitmarket.common.protocol.OfferId;
import com.bitwise.bitmarket.common.protocol.OfferType;
import com.bitwise.bitmarket.common.protocol.PeerId;

public class OfferConversions {

    public static Offer fromProtobuf(OfferProtocol.PublishOffer proto) {
        return new Offer(
                new OfferId(proto.getId()),
                fromProtobuf(proto.getType()),
                new PeerId(proto.getFrom()),
                PeerConnectionParser.parse(proto.getConnection()),
                new Amount(proto.getAmount(), Currency.getInstance("EUR"))); // TODO: fix this
    }

    public static OfferType fromProtobuf(OfferProtocol.OfferType offerType) {
        switch (offerType.getNumber()) {
            case OfferProtocol.OfferType.BUY_VALUE:
                return OfferType.BUY_OFFER;
            case OfferProtocol.OfferType.SELL_VALUE:
                return OfferType.SELL_OFFER;
            default:
                throw new IllegalArgumentException(String.format(
                        "no conversion defined of offer type %s from protobuf to internal representation",
                        offerType.toString()));
        }
    }

    public static OfferProtocol.PublishOffer toProtobuf(Offer offer) {
        return OfferProtocol.PublishOffer.newBuilder()
                .setId(offer.getId().getBytes())
                .setType(toProtobuf(offer.getOfferType()))
                .setFrom(offer.getFromId().getAddress())
                .setConnection(offer.getFromConnection().toString())
                .setAmount(Math.round(offer.getAmount().getAmount())) // TODO: fix this
                .build();
    }

    public static OfferProtocol.OfferType toProtobuf(OfferType offerType) {
        switch (offerType) {
            case BUY_OFFER: return OfferProtocol.OfferType.BUY;
            case SELL_OFFER: return OfferProtocol.OfferType.SELL;
            default:
                throw new IllegalArgumentException(String.format(
                        "no conversion defined of offer type %s from internal representation to protobuf",
                        offerType.toString()));
        }
    }

    private OfferConversions() {}
}
