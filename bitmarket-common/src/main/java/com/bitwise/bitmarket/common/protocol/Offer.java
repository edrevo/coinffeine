package com.bitwise.bitmarket.common.protocol;

import com.bitwise.bitmarket.common.PeerConnection;
import com.bitwise.bitmarket.common.currency.BtcAmount;
import com.bitwise.bitmarket.common.currency.FiatAmount;

public class Offer {

    private final OfferId id;
    private final int sequenceNumber;
    private final PeerId fromId;
    private final PeerConnection fromConnection;
    private final BtcAmount amount;
    private final FiatAmount bitcoinPrice;

    public Offer(
            OfferId id, int sequenceNumber, PeerId fromId, PeerConnection fromConnection,
            BtcAmount amount, FiatAmount bitcoinPrice) {
        this.id = id;
        this.sequenceNumber = sequenceNumber;
        this.fromId = fromId;
        this.fromConnection = fromConnection;
        this.amount = amount;
        this.bitcoinPrice = bitcoinPrice;
    }

    @Override
    public String toString() {
        return String.format("offer with ID %s from %s of %s (%s per BTC)",
                this.id.toString(),
                this.fromId.toString(),
                this.amount.toString(),
                this.bitcoinPrice.toString());
    }

    public OfferId getId() {
        return this.id;
    }

    public int getSequenceNumber() {
        return this.sequenceNumber;
    }

    public PeerId getFromId() {
        return this.fromId;
    }

    public PeerConnection getFromConnection() {
        return this.fromConnection;
    }

    public BtcAmount getAmount() {
        return this.amount;
    }

    public FiatAmount getBitcoinPrice() {
        return this.bitcoinPrice;
    }
}
