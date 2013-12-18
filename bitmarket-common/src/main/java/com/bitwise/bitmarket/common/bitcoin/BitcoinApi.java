package com.bitwise.bitmarket.common.bitcoin;

import java.security.interfaces.ECKey;

import com.bitwise.bitmarket.common.currency.BtcAmount;

public interface BitcoinApi {

    /**
     * Creates a special transaction called <a
     * href="https://en.bitcoin.it/wiki/Contracts">contract</a><br/>
     * 
     * The proposal of this transaction is create a transaction which contains
     * two deposits by each counterpart, the receiver is the same direction as
     * the sender on deposits for each counterpart. All included deposits are
     * commited at the same time.
     * 
     * The first deposit, is called Atomic deposit. This deposit is a multisign
     * transaction, just will be released when both counterparts will sign it
     * creating another transaction to this purpose.<br/>
     * 
     * The second deposit, is called Independent deposit. This deposit just will
     * be released when the counterpart will signs it creating another
     * transaction to this purpose.
     * 
     * This method returns a incomplete transaction (not valid to be broadcast)
     * with a inputs summatory amount minor than output. To make valid this
     * transaction, the counterpart must accept the contract, adding his atomic
     * and independent deposit.
     * 
     * For a broadcast Atomic Transaction, both counterparts must sign the
     * transaction to release the funds. When the funds are released each
     * counterpart will receive the deposit that them provided.
     * 
     * The negotiatedFee parameter included by each counterpart is a amount
     * negotiated between both in order to satisfy the Bitcoin network fee. The
     * bitcoin network fee is paid between both counterparts.
     * 
     * @param atomicDeposit
     *            FiatAmount for atomic deposit.
     * @param independentDeposit
     *            FiatAmount for independent deposit.
     * @param negotiatedFee
     *            Optimal fee negotiated with counterpart.
     * @param counterPart
     *            Counterpart public key.
     * @return Incomplete transaction (not valid to be broadcast) with a inputs
     *         summatory amount minor than output. To make valid this
     *         transaction, the counterpart must accept the contract, adding his
     *         atomic and independent deposit.
     */
    Contract createContract(BtcAmount atomicDeposit, BtcAmount independentDeposit,
            BtcAmount negotiatedFee, ECKey counterPart);

    /**
     * Broadcast the transaction received as a parameter to the Blockchain.
     * 
     * @param transaction
     *            Transaction to broadcast.
     * @return Transaction id.
     * @throws BitcoinException
     *             if the transaction to broadcast is not valid.
     */
    String broadcast(BtcTransaction transaction);

    /**
     * Gets the summatory of all inputs that could be spent.
     * 
     * @param confirmations
     *            Minimum confirmations number (0 to see balance including
     *            transactions which are not included in the Blockchain yet).
     * @return balance of the private key.
     */
    BtcAmount getBalance(int confirmations);

    /**
     * Creates and broadcast a transaction sending money to a receiver.
     * 
     * @param amount
     *            FiatAmount of Bitcoins to Send.
     * @param receiver
     *            Receiver Public Key.
     * @return Id of the transaction broadcast to the Blockchain.
     */
    String send(BtcAmount amount, ECKey receiver);

    /**
     * Returns the optimal fee to use on the transactions.
     * 
     * @return Fee.
     */
    BtcAmount getFee();
}
