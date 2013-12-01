package com.bitwise.bitmarket.common.protocol.protobuf;

import java.math.BigDecimal;
import java.util.Currency;

import com.bitwise.bitmarket.common.PeerConnection;
import com.bitwise.bitmarket.common.currency.Amount;
import com.bitwise.bitmarket.common.protocol.*;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ProtobufConversionsTest {

    @Test
    public void mustConvertOfferFromProtobuf() throws Exception {
        BitmarketProtobuf.PublishOffer protoOffer = BitmarketProtobuf.PublishOffer.newBuilder()
                .setId(1234567890)
                .setSeq(0)
                .setType(BitmarketProtobuf.OfferType.BUY)
                .setFrom("abcdefghijklmnopqrsruvwxyz")
                .setConnection("bitmarket://example.com:1234/")
                .setAmount(BitmarketProtobuf.Amount.newBuilder()
                        .setValue(2)
                        .setScale(0)
                        .setCurrency("EUR")
                        .build())
                .build();

        Offer offer = ProtobufConversions.fromProtobuf(protoOffer);

        assertEquals(1234567890, offer.getId().getBytes());
        assertEquals(0, offer.getSequenceNumber());
        assertEquals(OfferType.BUY_OFFER, offer.getOfferType());
        assertEquals("abcdefghijklmnopqrsruvwxyz", offer.getFromId().getAddress());
        assertEquals("bitmarket://example.com:1234/", offer.getFromConnection().toString());
        assertEquals(new BigDecimal(2), offer.getAmount().getAmount());
        assertEquals("EUR", offer.getAmount().getCurrency().getCurrencyCode());
    }

    @Test
    public void mustConvertOfferToProtobuf() throws Exception {
        Offer offer = new Offer(
                new OfferId(1234567890),
                0,
                OfferType.BUY_OFFER,
                new PeerId("abcdefghijklmnopqrsruvwxyz"),
                new PeerConnection("example.com", 1234),
                new Amount(new BigDecimal(2), Currency.getInstance("EUR")));

        BitmarketProtobuf.PublishOffer protoOffer = ProtobufConversions.toProtobuf(offer);

        assertEquals(1234567890, protoOffer.getId());
        assertEquals(0, protoOffer.getSeq());
        assertEquals(BitmarketProtobuf.OfferType.BUY, protoOffer.getType());
        assertEquals("abcdefghijklmnopqrsruvwxyz", protoOffer.getFrom());
        assertEquals("bitmarket://example.com:1234/", protoOffer.getConnection());
        assertEquals(2, protoOffer.getAmount().getValue());
        assertEquals(0, protoOffer.getAmount().getScale());
        assertEquals("EUR", protoOffer.getAmount().getCurrency());
    }

    @Test
    public void mustConvertOfferToProtoAndBackAgain() throws Exception {
        Offer offer = new Offer(
                new OfferId(1234567890),
                0,
                OfferType.BUY_OFFER,
                new PeerId("abcdefghijklmnopqrsruvwxyz"),
                new PeerConnection("example.com", 1234),
                new Amount(new BigDecimal(2), Currency.getInstance("EUR")));

        BitmarketProtobuf.PublishOffer protoOffer = ProtobufConversions.toProtobuf(offer);
        Offer newOffer = ProtobufConversions.fromProtobuf(protoOffer);

        assertEquals(1234567890, newOffer.getId().getBytes());
        assertEquals(0, newOffer.getSequenceNumber());
        assertEquals(OfferType.BUY_OFFER, newOffer.getOfferType());
        assertEquals("abcdefghijklmnopqrsruvwxyz", newOffer.getFromId().getAddress());
        assertEquals("bitmarket://example.com:1234/", newOffer.getFromConnection().toString());
        assertEquals(new BigDecimal(2), newOffer.getAmount().getAmount());
        assertEquals("EUR", newOffer.getAmount().getCurrency().getCurrencyCode());
    }

    @Test
    public void mustConvertExchangeRequestFromProtobuf() throws Exception {
        BitmarketProtobuf.ExchangeRequest protoRequest =
                BitmarketProtobuf.ExchangeRequest.newBuilder()
                    .setId(1234567890)
                    .setFrom("abcdefghijklmnopqrsruvwxyz")
                    .setConnection("bitmarket://example.com:1234/")
                    .setAmount(
                            BitmarketProtobuf.Amount
                                    .newBuilder()
                                    .setValue(2)
                                    .setScale(0)
                                    .setCurrency("EUR")
                                    .build())
                    .build();

        ExchangeRequest request = ProtobufConversions.fromProtobuf(protoRequest);

        assertEquals(1234567890, request.getId().getBytes());
        assertEquals("abcdefghijklmnopqrsruvwxyz", request.getFromId().getAddress());
        assertEquals("bitmarket://example.com:1234/", request.getFromConnection().toString());
        assertEquals(new BigDecimal(2), request.getAmount().getAmount());
        assertEquals("EUR", request.getAmount().getCurrency().getCurrencyCode());
    }

    @Test
    public void mustConvertExchangeRequestToProtobuf() throws Exception {
        ExchangeRequest request = new ExchangeRequest(
                new OfferId(1234567890),
                new PeerId("abcdefghijklmnopqrsruvwxyz"),
                new PeerConnection("example.com", 1234),
                new Amount(new BigDecimal(2), Currency.getInstance("EUR")));

        BitmarketProtobuf.ExchangeRequest protoRequest = ProtobufConversions.toProtobuf(request);

        assertEquals(1234567890, protoRequest.getId());
        assertEquals("abcdefghijklmnopqrsruvwxyz", protoRequest.getFrom());
        assertEquals("bitmarket://example.com:1234/", protoRequest.getConnection());
        assertEquals(2, protoRequest.getAmount().getValue());
        assertEquals(0, protoRequest.getAmount().getScale());
        assertEquals("EUR", protoRequest.getAmount().getCurrency());
    }

    @Test
    public void mustConvertExchangeRequestToProtoAndBackAgain() throws Exception {
        ExchangeRequest request = new ExchangeRequest(
                new OfferId(1234567890),
                new PeerId("abcdefghijklmnopqrsruvwxyz"),
                new PeerConnection("example.com", 1234),
                new Amount(new BigDecimal(2), Currency.getInstance("EUR")));

        BitmarketProtobuf.ExchangeRequest protoRequest = ProtobufConversions.toProtobuf(request);
        ExchangeRequest newRequest = ProtobufConversions.fromProtobuf(protoRequest);

        assertEquals(1234567890, newRequest.getId().getBytes());
        assertEquals("abcdefghijklmnopqrsruvwxyz", newRequest.getFromId().getAddress());
        assertEquals("bitmarket://example.com:1234/", newRequest.getFromConnection().toString());
        assertEquals(new BigDecimal(2), newRequest.getAmount().getAmount());
        assertEquals("EUR", newRequest.getAmount().getCurrency().getCurrencyCode());
    }
}
