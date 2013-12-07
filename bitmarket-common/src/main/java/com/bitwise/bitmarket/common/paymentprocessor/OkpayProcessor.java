package com.bitwise.bitmarket.common.paymentprocessor;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.bitwise.bitmarket.common.paymentprocessor.okpay.OkPayAPIImplementationStub;
import com.bitwise.bitmarket.common.paymentprocessor.okpay.OkPayAPIImplementationStub.ArrayOfBalance;
import com.bitwise.bitmarket.common.paymentprocessor.okpay.OkPayAPIImplementationStub.Balance;
import com.bitwise.bitmarket.common.paymentprocessor.okpay.OkPayAPIImplementationStub.HistoryInfo;
import com.bitwise.bitmarket.common.paymentprocessor.okpay.OkPayAPIImplementationStub.OperationStatus;
import com.bitwise.bitmarket.common.paymentprocessor.okpay.OkPayAPIImplementationStub.Send_Money;
import com.bitwise.bitmarket.common.paymentprocessor.okpay.OkPayAPIImplementationStub.Send_MoneyResponse;
import com.bitwise.bitmarket.common.paymentprocessor.okpay.OkPayAPIImplementationStub.TransactionInfo;
import com.bitwise.bitmarket.common.paymentprocessor.okpay.OkPayAPIImplementationStub.Transaction_History;
import com.bitwise.bitmarket.common.paymentprocessor.okpay.OkPayAPIImplementationStub.Transaction_HistoryResponse;
import com.bitwise.bitmarket.common.paymentprocessor.okpay.OkPayAPIImplementationStub.Wallet_Get_Balance;
import com.bitwise.bitmarket.common.paymentprocessor.okpay.OkPayAPIImplementationStub.Wallet_Get_BalanceResponse;
import com.bitwise.bitmarket.common.currency.FiatAmount;

class OkPayProcessor implements PaymentProcessor {

    private static final String OKPAY_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String TOKEN_ENCODING = "UTF-8";
    private static final DateTimeFormatter receivedDateFormatter = DateTimeFormat
            .forPattern(OKPAY_DATE_FORMAT);
    private final OkPayAPIImplementationStub okpayService;
    private final String token;
    private final String userId;

    public OkPayProcessor(OkPayAPIImplementationStub okpayService,
            String token, String userId) {
        this.okpayService = okpayService;
        this.token = token;
        this.userId = userId;
    }

    @Override
    public Payment sendPayment(String receiverId, FiatAmount amount)
            throws PaymentProcessorException {
        Send_Money params = OkPayProcessor.buildSendPaymentParams(receiverId,
                amount, this.userId, currentTokenBuilder(this.token));
        try {
            Send_MoneyResponse response = this.okpayService.send_Money(params);
            return buildPaymentFromTransaction(response.getSend_MoneyResult());
        } catch (RemoteException e) {
            throw new PaymentProcessorException(e.getMessage(), e);
        }
    }

    @Override
    public boolean checkPayment(String senderId, FiatAmount amount,
            DateTime fromDate) throws PaymentProcessorException {
        Transaction_History params = buildCheckPaymentParams(this.userId,
                fromDate, currentTokenBuilder(this.token));
        try {
            Transaction_HistoryResponse response = this.okpayService
                    .transaction_History(params);
            if (parseCheckPaymentResponse(senderId, response).size() > 0) {
                return true;
            } else {
                return false;
            }
        } catch (RemoteException e) {
            throw new PaymentProcessorException(e.getMessage(), e);
        }
    }

    @Override
    public Iterable<FiatAmount> getBalance() throws PaymentProcessorException {
        Wallet_Get_Balance params = OkPayProcessor.buildGetBalanceParams(
                this.userId, currentTokenBuilder(this.token));
        try {
            Wallet_Get_BalanceResponse response = this.okpayService
                    .wallet_Get_Balance(params);
            return parseGetBalanceResponse(response);
        } catch (RemoteException e) {
            throw new PaymentProcessorException(e.getMessage(), e);
        }
    }

    private static final Iterable<FiatAmount> parseGetBalanceResponse(
            Wallet_Get_BalanceResponse response) {
        return buildBalance(response.getWallet_Get_BalanceResult());
    }

    private static final List<Payment> parseCheckPaymentResponse(
            String senderId, Transaction_HistoryResponse response) {
        HistoryInfo history = response.getTransaction_HistoryResult();
        List<Payment> payments = new ArrayList<Payment>();
        for (TransactionInfo transaction : history.getTransactions()
                .getTransactionInfo()) {
            Payment payment = buildPaymentFromTransaction(transaction);
            String status = transaction.getStatus().getValue();
            if (status.equals(OperationStatus.Completed.toString())
                    && payment.getSenderId().equals(senderId)) {
                payments.add(payment);
            }
        }
        return payments;
    }

    private static final Iterable<FiatAmount> buildBalance(ArrayOfBalance balances) {
        List<FiatAmount> amounts = new ArrayList<FiatAmount>();
        for (Balance balance : balances.getBalance()) {
            FiatAmount amount = new FiatAmount(balance.getAmount(),
                    Currency.getInstance(balance.getCurrency()));
            amounts.add(amount);
        }
        return amounts;
    }

    private static final Payment buildPaymentFromTransaction(
            TransactionInfo transaction) {
        String senderId = transaction.getSender().getWalletID();
        String receiverId = transaction.getReceiver().getWalletID();
        BigDecimal amountValue = transaction.getAmount();
        Currency currency = Currency.getInstance(transaction.getCurrency());
        FiatAmount amount = new FiatAmount(amountValue, currency);
        String description = transaction.getComment();
        DateTime date = receivedDateFormatter.parseDateTime(transaction
                .getDate());
        String id = Long.toString(transaction.getID());
        return new Payment(senderId, receiverId, amount, date, description, id);
    }

    private static final Transaction_History buildCheckPaymentParams(
            String userId, DateTime fromDate, String currentToken) {
        Transaction_History params = new Transaction_History();
        params.setFrom(fromDate.toString(OKPAY_DATE_FORMAT));
        params.setTill(DateTime.now().toString(OKPAY_DATE_FORMAT));
        params.setWalletID(userId);
        params.setPageNumber(1);
        params.setPageSize(20);
        params.setSecurityToken(currentToken);
        return params;
    }

    private static final Send_Money buildSendPaymentParams(String receiverId,
            FiatAmount amount, String userId, String currentToken) {
        Send_Money params = new Send_Money();
        params.setAmount(amount.getAmount());
        params.setCurrency(amount.getCurrency().getCurrencyCode().toString());
        params.setReceiver(receiverId);
        params.setWalletID(userId);
        params.setSecurityToken(currentToken);
        return params;
    }

    private static final Wallet_Get_Balance buildGetBalanceParams(
            String userId, String currentToken) {
        Wallet_Get_Balance params = new Wallet_Get_Balance();
        params.setWalletID(userId);
        params.setSecurityToken(currentToken);
        return params;
    }

    private static final String currentTokenBuilder(String token) {
        try {
            String date = DateTime.now(DateTimeZone.UTC).toString("yyyyMMdd");
            String hour = DateTime.now(DateTimeZone.UTC).toString("HH");
            String currentToken = String.format("%s:%s:%s", token, date, hour);
            return hash(currentToken.getBytes(TOKEN_ENCODING));
        } catch (UnsupportedEncodingException e) {
            throw new PaymentProcessorException(e.getMessage(), e);
        }
    }

    private static final String hash(byte[] byteArray) {
        try {
            StringBuffer hash = new StringBuffer();
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    byteArray);
            for (byte position : digest) {
                hash.append(String.format("%02x", position));
            }
            return hash.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new PaymentProcessorException(e.getMessage(), e);
        }
    }
}
