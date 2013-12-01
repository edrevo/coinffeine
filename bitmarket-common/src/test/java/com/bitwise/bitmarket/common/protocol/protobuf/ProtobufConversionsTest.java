package com.bitwise.bitmarket.common.protocol.protobuf;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.Test;

import com.bitwise.bitmarket.common.PeerConnection;
import com.bitwise.bitmarket.common.currency.BtcAmount;
import com.bitwise.bitmarket.common.currency.FiatAmount;
import com.bitwise.bitmarket.common.protocol.*;

import static org.junit.Assert.assertEquals;

public class ProtobufConversionsTest {

    @Test
    public void mustConvertOfferFromProtobuf() throws Exception {
        BitmarketProtobuf.Offer protoOffer = BitmarketProtobuf.Offer.newBuilder()
                .setId(1234567890)
                .setSeq(0)
                .setFrom("abcdefghijklmnopqrsruvwxyz")
                .setConnection("bitmarket://example.com:1234/")
                .setAmount(
                        BitmarketProtobuf.BtcAmount.newBuilder().setValue(2).setScale(0).build())
                .setBtcPrice(
                        BitmarketProtobuf.FiatAmount
                                .newBuilder()
                                .setValue(100)
                                .setScale(0)
                                .setCurrency("EUR")
                                .build())
                .build();

        Offer offer = ProtobufConversions.fromProtobuf(protoOffer);

        assertEquals(1234567890, offer.getId().getBytes());
        assertEquals(0, offer.getSequenceNumber());
        assertEquals("abcdefghijklmnopqrsruvwxyz", offer.getFromId().getAddress());
        assertEquals("bitmarket://example.com:1234/", offer.getFromConnection().toString());
        assertEquals(new BigDecimal(2), offer.getAmount().getAmount());
        assertEquals(new BigDecimal(100), offer.getBitcoinPrice().getAmount());
        assertEquals("EUR", offer.getBitcoinPrice().getCurrency().getCurrencyCode());
    }

    @Test
    public void mustConvertOfferToProtobuf() throws Exception {
        Offer offer = new Offer(
                new OfferId(1234567890),
                0,
                new PeerId("abcdefghijklmnopqrsruvwxyz"),
                new PeerConnection("example.com", 1234),
                new BtcAmount(new BigDecimal(2)),
                new FiatAmount(new BigDecimal(2), Currency.getInstance("EUR")));

        BitmarketProtobuf.Offer protoOffer = ProtobufConversions.toProtobuf(offer);

        assertEquals(1234567890, protoOffer.getId());
        assertEquals(0, protoOffer.getSeq());
        assertEquals("abcdefghijklmnopqrsruvwxyz", protoOffer.getFrom());
        assertEquals("bitmarket://example.com:1234/", protoOffer.getConnection());
        assertEquals(2, protoOffer.getAmount().getValue());
        assertEquals(0, protoOffer.getAmount().getScale());
    }

    @Test
    public void mustConvertOfferToProtoAndBackAgain() throws Exception {
        Offer offer = new Offer(
                new OfferId(1234567890),
                0,
                new PeerId("abcdefghijklmnopqrsruvwxyz"),
                new PeerConnection("example.com", 1234),
                new BtcAmount(new BigDecimal(2)),
                new FiatAmount(new BigDecimal(100), Currency.getInstance("EUR")));

        BitmarketProtobuf.Offer protoOffer = ProtobufConversions.toProtobuf(offer);
        Offer newOffer = ProtobufConversions.fromProtobuf(protoOffer);

        assertEquals(1234567890, newOffer.getId().getBytes());
        assertEquals(0, newOffer.getSequenceNumber());
        assertEquals("abcdefghijklmnopqrsruvwxyz", newOffer.getFromId().getAddress());
        assertEquals("bitmarket://example.com:1234/", newOffer.getFromConnection().toString());
        assertEquals(new BigDecimal(2), newOffer.getAmount().getAmount());
        assertEquals(new BigDecimal(100), newOffer.getBitcoinPrice().getAmount());
        assertEquals("EUR", newOffer.getBitcoinPrice().getCurrency().getCurrencyCode());
    }

    @Test
    public void mustConvertExchangeRequestFromProtobuf() throws Exception {
        BitmarketProtobuf.ExchangeRequest protoRequest =
                BitmarketProtobuf.ExchangeRequest.newBuilder()
                    .setId(1234567890)
                    .setFrom("abcdefghijklmnopqrsruvwxyz")
                    .setConnection("bitmarket://example.com:1234/")
                    .setAmount(
                            BitmarketProtobuf.BtcAmount
                                    .newBuilder()
                                    .setValue(2)
                                    .setScale(0)
                                    .build())
                    .build();

        ExchangeRequest request = ProtobufConversions.fromProtobuf(protoRequest);

        assertEquals(1234567890, request.getId().getBytes());
        assertEquals("abcdefghijklmnopqrsruvwxyz", request.getFromId().getAddress());
        assertEquals("bitmarket://example.com:1234/", request.getFromConnection().toString());
        assertEquals(new BigDecimal(2), request.getAmount().getAmount());
    }

    @Test
    public void mustConvertExchangeRequestToProtobuf() throws Exception {
        ExchangeRequest request = new ExchangeRequest(
                new OfferId(1234567890),
                new PeerId("abcdefghijklmnopqrsruvwxyz"),
                new PeerConnection("example.com", 1234),
                new BtcAmount(new BigDecimal(2)));

        BitmarketProtobuf.ExchangeRequest protoRequest = ProtobufConversions.toProtobuf(request);

        assertEquals(1234567890, protoRequest.getId());
        assertEquals("abcdefghijklmnopqrsruvwxyz", protoRequest.getFrom());
        assertEquals("bitmarket://example.com:1234/", protoRequest.getConnection());
        assertEquals(2, protoRequest.getAmount().getValue());
        assertEquals(0, protoRequest.getAmount().getScale());
    }

    @Test
    public void mustConvertExchangeRequestToProtoAndBackAgain() throws Exception {
        ExchangeRequest request = new ExchangeRequest(
                new OfferId(1234567890),
                new PeerId("abcdefghijklmnopqrsruvwxyz"),
                new PeerConnection("example.com", 1234),
                new BtcAmount(new BigDecimal(2)));

        BitmarketProtobuf.ExchangeRequest protoRequest = ProtobufConversions.toProtobuf(request);
        ExchangeRequest newRequest = ProtobufConversions.fromProtobuf(protoRequest);

        assertEquals(1234567890, newRequest.getId().getBytes());
        assertEquals("abcdefghijklmnopqrsruvwxyz", newRequest.getFromId().getAddress());
        assertEquals("bitmarket://example.com:1234/", newRequest.getFromConnection().toString());
        assertEquals(new BigDecimal(2), newRequest.getAmount().getAmount());
    }
}
