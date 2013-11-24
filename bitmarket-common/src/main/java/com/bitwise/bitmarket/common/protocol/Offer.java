package com.bitwise.bitmarket.common.protocol;

import com.bitwise.bitmarket.common.PeerConnection;
import com.bitwise.bitmarket.common.currency.Amount;

public class Offer {

    private final OfferId id;
    private final int sequenceNumber;
    private final OfferType offerType;
    private final PeerId fromId;
    private final PeerConnection fromConnection;
    private final Amount amount;

    public Offer(
            OfferId id, int sequenceNumber, OfferType offerType, PeerId fromId,
            PeerConnection fromConnection, Amount amount) {
        this.id = id;
        this.sequenceNumber = sequenceNumber;
        this.offerType = offerType;
        this.fromId = fromId;
        this.fromConnection = fromConnection;
        this.amount = amount;
    }

    @Override
    public String toString() {
        return String.format("%s offer with ID %s from %s of %s",
                this.offerType.toString(),
                this.id.toString(),
                this.fromId.toString(),
                this.amount.toString());
    }

    public OfferId getId() {
        return this.id;
    }

    public int getSequenceNumber() {
        return this.sequenceNumber;
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
