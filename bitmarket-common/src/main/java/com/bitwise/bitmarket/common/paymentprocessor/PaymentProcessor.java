package com.bitwise.bitmarket.common.paymentprocessor;

import org.joda.time.DateTime;

import com.bitwise.bitmarket.common.currency.FiatAmount;

public interface PaymentProcessor {

    /**
     * Send a payment from any of your wallets to someone.
     * 
     * @param receiverId
     *            account id of receiver of payment
     * @param amount
     *            amount to send
     * @return a Payment object containing the information of payment (receiverId and senderId
     *         properties are not provided)
     * @throws PaymentProcessorException
     *             if the payment can not be made
     */
    Payment sendPayment(String receiverId, FiatAmount amount)
            throws PaymentProcessorException;

    /**
     * Check if exist a payment with a some specific details (from a specific account, with a
     * specific amount, etc.) from a date.
     * 
     * @param receiverId
     *            account id who expect to receive the payment
     * @param amount
     *            amount of money that you will expect to receive
     * @param fromDate
     *            date from which you expect the payment
     * @return boolean value that indicates if the payment was made
     * @throws PaymentProcessorException
     *             if the payment can not be made
     */
    boolean checkPayment(String senderId, FiatAmount amount, DateTime fromDate)
            throws PaymentProcessorException;

    /**
     * Return the balance for a wallet on your account.
     * 
     * @return List of Amounts, one amount per currency type in the account
     * @throws PaymentProcessorException
     *             if the payment can not be made
     */
    Iterable<FiatAmount> getBalance() throws PaymentProcessorException;
}
