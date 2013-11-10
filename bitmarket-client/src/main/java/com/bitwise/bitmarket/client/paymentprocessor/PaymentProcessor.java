package com.bitwise.bitmarket.client.paymentprocessor;

import org.joda.time.DateTime;

import com.bitwise.bitmarket.common.currency.Amount;

public interface PaymentProcessor {

    /**
     * Send a payment from any of your wallets to someone.
     * 
     * @param toAccountId
     *            who will receive the payment
     * @param amount
     *            to send
     * @param wallet
     *            in your account to use to send the payment
     * @return transactionId of the order
     * @throws Exception
     *             if the payment can not be made
     */
    String sendPayment(String toAccountId, Amount amount, String fromWalletId)
            throws PaymentProcessorException;

    /**
     * Check if exist a payment with a some specific details (from a specific account, with a
     * specific amount, etc.) from a date.
     * 
     * @param walletId
     *            in which you expect to receive the payment
     * @param Account
     *            ID from you expect receive the payment
     * @param amount
     *            of money that you will expect to receive
     * @param Date
     *            from which you expect the payment
     * @return true if the payment exist and false if not exist.
     */
    boolean checkPayment(String walletId, String fromAccountId, Amount amount,
            DateTime fromDate) throws PaymentProcessorException;;

    /**
     * Return the balance for a wallet on your account.
     * 
     * @param walletId
     *            of your wallet
     * @return List of amounts in your wallet by currency
     */
    Iterable<Amount> getBalance(String walletId)
            throws PaymentProcessorException;
}
