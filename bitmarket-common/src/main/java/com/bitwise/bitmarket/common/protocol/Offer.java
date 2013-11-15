package com.bitwise.bitmarket.common.protocol;

import com.bitwise.bitmarket.common.PeerConnection;
import com.bitwise.bitmarket.common.currency.Amount;

public class Offer {

    private final OfferId id;
    private final OfferType offerType;
    private final PeerId fromId;
    private final PeerConnection fromConnection;
    private final Amount amount;

    public Offer(
            OfferId id, OfferType offerType, PeerId fromId,
            PeerConnection fromConnection, Amount amount) {
        this.id = id;
        this.offerType = offerType;
        this.fromId = fromId;
        this.fromConnection = fromConnection;
        this.amount = amount;
    }

    public OfferId getId() {
        return this.id;
    }

    public OfferType getOfferType() {
        return this.offerType;
    }

    public PeerId getFromId() {
        return this.fromId;
    }

    public PeerConnection getFromConnection() {
        return this.fromConnection;
    }

    public Amount getAmount() {
        return this.amount;
    }
}
