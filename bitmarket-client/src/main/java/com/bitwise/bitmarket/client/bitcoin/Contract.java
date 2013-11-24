package com.bitwise.bitmarket.client.bitcoin;

import java.math.BigDecimal;
import java.security.interfaces.ECKey;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.bitwise.bitmarket.common.currency.BtcAmount;

public abstract class Contract implements Transaction {

    private final byte[] rawTransaction;
    private final List<TransactionSlot> inputs;
    private final List<TransactionSlot> outputs;
    private final String id;
    private final int confirmations;

    Contract(byte[] rawTransaction, List<TransactionSlot> inputs, List<TransactionSlot> outputs,
            @Nullable String id, int confirmations) {
        this.rawTransaction = rawTransaction;
        this.inputs = inputs;
        this.outputs = outputs;
        this.id = id;
        this.confirmations = confirmations;
    }

    @Override
    public byte[] getRawTransaction() {
        return this.rawTransaction;
    }

    @Override
    public Iterable<TransactionSlot> getInputs() {
        return new ArrayList<TransactionSlot>(this.inputs);
    }

    @Override
    public Iterable<TransactionSlot> getOutputs() {
        return new ArrayList<TransactionSlot>(this.outputs);
    }

    @Override
    @Nullable
    public String getId() {
        return this.id;
    }

    @Override
    @Nullable
    public int getConfirmations() {
        return this.confirmations;
    }

    @Override
    public BtcAmount getTotalOutputAmount() {
        return new BtcAmount(getSum(this.outputs));
    }

    @Override
    public BtcAmount getTotalInputAmount() {
        return new BtcAmount(getSum(this.inputs));
    }

    @Override
    public BtcAmount getFee() {
        return new BtcAmount(getSum(this.outputs).subtract(getSum(this.inputs)));
    }

    private static BigDecimal getSum(Iterable<TransactionSlot> slots) {
        BigDecimal sum = new BigDecimal(0);
        for (final TransactionSlot txslot : slots) {
            sum = sum.add(txslot.getAmount());
        }
        return sum;
    }

    /**
     * This method accepts a contract, adding the atomic and independent amount
     * deposits specified as a parameters.<br/>
     * 
     * Receive as a parameter a incomplete transaction (not valid to be
     * broadcasted) which includes independent and atomic deposit amounts signed
     * from a user. The method adds your atomic and independent amount deposits
     * to the transaction and returns a valid transaction ready to be
     * broadcasted.<br/>
     * 
     * The negotiatedFee parameter included by each counterpart is a amount
     * negotiated between both in order to satisfy the Bitcoin network fee. The
     * bitcoin network fee is paid between both counterparts.
     * 
     * @param atomicAmount
     *            Amount for atomic deposit.
     * @param independentAmount
     *            Amount for independent deposit.
     * @param negotiatedFee
     *            fee negotiated with counterpart in order to pay the Bitcoin
     *            network fee.
     * @param counterPart
     *            counterpart public key.
     * @param contract
     *            Partial contract from counterpart which adds the deposits.
     *            Counterpart public key.
     */
    public abstract void acceptContract(BtcAmount atomicAmount, BtcAmount independentAmount,
            BtcAmount negotiatedFee, ECKey counterPart, Transaction contract);

    /**
     * Signs part of the contract to release the atomic deposit and return a
     * transaction signed ready to be broadcasted. The Atomic deposit just be
     * released if both counterparts sign.
     * 
     * @param PrivateKeyToSign
     *            Private key to sign the atomic deposit.
     * 
     * @throws ContractException
     *             if the key provided can't sign any atomic deposit.
     */
    public abstract void signAtomicDeposit(ECKey PrivateKeyToSign);

    /**
     * Signs (but don't broadcast) a deposit from the counterpart in order to
     * release his funds.
     * 
     * @param PrivateKeyToSign
     *            Private key to sign the atomic deposit.
     * 
     * @throws ContractException
     *             if the key provided can't sign any independent deposit.
     */
    public abstract void signIndependentDeposit(ECKey PrivateKeyToSign);
}
