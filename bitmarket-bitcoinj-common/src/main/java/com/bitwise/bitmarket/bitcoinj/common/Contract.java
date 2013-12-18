package com.bitwise.bitmarket.bitcoinj.common;

import java.math.BigDecimal;
import java.security.interfaces.ECKey;

import javax.annotation.Nullable;

import com.bitwise.bitmarket.common.currency.BtcAmount;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.ProtocolException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;

public abstract class Contract implements BtcTransaction {

    private final Transaction transaction;
    private final static BigDecimal ONE_BILLION = new BigDecimal(1000000000);

    Contract(NetworkParameters network, byte[] rawTransaction) {
        try {
            this.transaction = new Transaction(network, rawTransaction);
        } catch (ProtocolException e) {
            throw new BitcoinException(e);
        }
    }

    @Override
    public byte[] getRawTransaction() {
        return this.transaction.bitcoinSerialize();
    }

    @Override
    public Iterable<TransactionInput> getInputs() {
        return this.transaction.getInputs();
    }

    @Override
    public Iterable<TransactionOutput> getOutputs() {
        return this.transaction.getOutputs();
    }

    @Override
    @Nullable
    public String getId() {
        return this.transaction.getHashAsString();
    }

    @Override
    @Nullable
    public int getConfirmations() {
        return this.transaction.getConfidence().getDepthInBlocks();
    }

    @Override
    public BtcAmount getTotalOutputAmount() {
        BigDecimal amount = new BigDecimal(0);
        for (TransactionOutput output : this.transaction.getOutputs()) {
            amount.add(new BigDecimal(output.getValue()));
        }
        return new BtcAmount(amount.multiply(ONE_BILLION));
    }

    @Override
    public BtcAmount getTotalInputAmount() {
        BigDecimal amount = new BigDecimal(0);
        for (TransactionInput input : this.transaction.getInputs()) {
            amount.add(new BigDecimal(input.getOutpoint().getConnectedOutput().getValue()));
        }
        return new BtcAmount(amount.multiply(ONE_BILLION));
    }

    @Override
    public BtcAmount getFee() {
        BigDecimal inputAmount = this.getTotalInputAmount().getAmount();
        BigDecimal outputAmount = this.getTotalOutputAmount().getAmount();
        return new BtcAmount(inputAmount.subtract(outputAmount));
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
     *            FiatAmount for atomic deposit.
     * @param independentAmount
     *            FiatAmount for independent deposit.
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
