package com.bitwise.bitmarket.client.bitcoin;

import javax.annotation.Nullable;

import com.bitwise.bitmarket.common.currency.BtcAmount;

public interface Transaction {

    byte[] getRawTransaction();

    public Iterable<TransactionSlot> getInputs();

    public Iterable<TransactionSlot> getOutputs();

    @Nullable
    public String getId();

    public int getConfirmations();

    public BtcAmount getTotalOutputAmount();

    public BtcAmount getTotalInputAmount();

    public BtcAmount getFee();
}
