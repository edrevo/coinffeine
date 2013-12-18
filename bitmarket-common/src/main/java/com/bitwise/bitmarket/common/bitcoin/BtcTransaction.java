package com.bitwise.bitmarket.common.bitcoin;

import javax.annotation.Nullable;

import com.bitwise.bitmarket.common.currency.BtcAmount;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;

public interface BtcTransaction {

    byte[] getRawTransaction();

    public Iterable<TransactionInput> getInputs();

    public Iterable<TransactionOutput> getOutputs();

    @Nullable
    public String getId();

    public int getConfirmations();

    public BtcAmount getTotalOutputAmount();

    public BtcAmount getTotalInputAmount();

    public BtcAmount getFee();
}
