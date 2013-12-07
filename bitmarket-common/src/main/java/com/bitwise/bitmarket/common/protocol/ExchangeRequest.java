package com.bitwise.bitmarket.common.protocol;

import com.bitwise.bitmarket.common.PeerConnection;
import com.bitwise.bitmarket.common.currency.BtcAmount;

public class ExchangeRequest {

    private final OfferId id;
    private final PeerId fromId;
    private final PeerConnection fromConnection;
    private final BtcAmount amount;

    public ExchangeRequest(
            OfferId id, PeerId fromId, PeerConnection fromConnection, BtcAmount amount) {
        this.id = id;
        this.fromId = fromId;
        this.fromConnection = fromConnection;
        this.amount = amount;
    }

    public OfferId getId() {
        return this.id;
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
}
