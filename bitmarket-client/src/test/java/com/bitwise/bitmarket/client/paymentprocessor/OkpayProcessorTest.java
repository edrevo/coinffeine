package com.bitwise.bitmarket.client.paymentprocessor;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import junit.framework.Assert;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.bitwise.bitmarket.client.paymentprocessor.okpay.OkPayAPIImplementationStub;
import com.bitwise.bitmarket.client.paymentprocessor.okpay.OkPayAPIImplementationStub.AccountInfo;
import com.bitwise.bitmarket.client.paymentprocessor.okpay.OkPayAPIImplementationStub.ArrayOfBalance;
import com.bitwise.bitmarket.client.paymentprocessor.okpay.OkPayAPIImplementationStub.ArrayOfTransactionInfo;
import com.bitwise.bitmarket.client.paymentprocessor.okpay.OkPayAPIImplementationStub.Balance;
import com.bitwise.bitmarket.client.paymentprocessor.okpay.OkPayAPIImplementationStub.HistoryInfo;
import com.bitwise.bitmarket.client.paymentprocessor.okpay.OkPayAPIImplementationStub.OperationStatus;
import com.bitwise.bitmarket.client.paymentprocessor.okpay.OkPayAPIImplementationStub.Send_Money;
import com.bitwise.bitmarket.client.paymentprocessor.okpay.OkPayAPIImplementationStub.Send_MoneyResponse;
import com.bitwise.bitmarket.client.paymentprocessor.okpay.OkPayAPIImplementationStub.TransactionInfo;
import com.bitwise.bitmarket.client.paymentprocessor.okpay.OkPayAPIImplementationStub.Transaction_History;
import com.bitwise.bitmarket.client.paymentprocessor.okpay.OkPayAPIImplementationStub.Transaction_HistoryResponse;
import com.bitwise.bitmarket.client.paymentprocessor.okpay.OkPayAPIImplementationStub.Wallet_Get_Balance;
import com.bitwise.bitmarket.client.paymentprocessor.okpay.OkPayAPIImplementationStub.Wallet_Get_BalanceResponse;
import com.bitwise.bitmarket.common.currency.Amount;

@SuppressWarnings("deprecation")
@RunWith(MockitoJUnitRunner.class)
public class OkpayProcessorTest {

    private final static String OKPAY_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private final DateTimeFormatter formatter = DateTimeFormat
            .forPattern(OKPAY_DATE_FORMAT);

    private OkPayProcessor instance;

    private Payment payment1;
    private Payment payment2;

    @Mock
    OkPayAPIImplementationStub okpayService;

    @Before
    public void configure() {
        this.instance = new OkPayProcessor(this.okpayService,
                "Sr24Hka6TNe79Qmf8PKx35EsR", "OK123456");
        this.payment1 = new Payment("OK123456", "OK12345", new Amount(
                new BigDecimal(5), Currency.getInstance("EUR")),
                this.formatter.parseDateTime("2013-10-10 12:12:12"), "desc1",
                "1234");
        this.payment2 = new Payment("OK123456", "OK12346", new Amount(
                new BigDecimal(6), Currency.getInstance("EUR")),
                this.formatter.parseDateTime("2013-10-10 12:12:12"), "desc1",
                "1234");
    }

    @Test
    public void sendPaymentTest() throws RemoteException {
        TransactionInfo transaction = OkpayProcessorTest
                .buildTransactionInfoMock(this.payment1);
        Send_MoneyResponse response = Mockito.mock(Send_MoneyResponse.class);
        Mockito.when(response.getSend_MoneyResult()).thenReturn(transaction);
        Mockito.when(
                this.okpayService.send_Money(Matchers.any(Send_Money.class)))
                .thenReturn(response);
        Payment payment = this.instance.sendPayment("OK12345", new Amount(
                new BigDecimal(5), Currency.getInstance("EUR")));
        Assert.assertEquals(this.payment1, payment);
    }

    @Test
    public void CheckPaymentTest() throws RemoteException {
        Transaction_HistoryResponse response = Mockito
                .mock(Transaction_HistoryResponse.class);
        HistoryInfo history = buildTransactionHistoryMock(this.payment1,
                this.payment2);
        Mockito.when(response.getTransaction_HistoryResult()).thenReturn(
                history);
        Mockito.when(
                this.okpayService.transaction_History(Matchers
                        .any(Transaction_History.class))).thenReturn(response);
        Assert.assertTrue(this.instance.checkPayment(
                this.payment1.getSenderId(),
                this.payment1.getAmount(),
                this.formatter.parseDateTime(this.payment1.getDate().toString(
                        OKPAY_DATE_FORMAT))));
    }

    @Test
    public void getBalanceTest() throws RemoteException {
        Amount amount1 = new Amount(new BigDecimal(1),
                Currency.getInstance("EUR"));
        Amount amount2 = new Amount(new BigDecimal(2),
                Currency.getInstance("USD"));
        ArrayOfBalance arrayOfBalance = buildArrayOfBalanceMock(amount1,
                amount2);
        Wallet_Get_BalanceResponse response = Mockito
                .mock(Wallet_Get_BalanceResponse.class);
        Mockito.when(response.getWallet_Get_BalanceResult()).thenReturn(
                arrayOfBalance);
        Mockito.when(
                this.okpayService.wallet_Get_Balance(Matchers
                        .any(Wallet_Get_Balance.class))).thenReturn(response);
        List<Amount> balance = (List<Amount>) this.instance.getBalance();
        Assert.assertEquals(amount1, balance.get(0));
        Assert.assertEquals(amount2, balance.get(1));
    }

    private static final ArrayOfBalance buildArrayOfBalanceMock(
            Amount... amounts) {
        ArrayOfBalance arrayOfBalance = Mockito.mock(ArrayOfBalance.class);
        List<Balance> balances = new ArrayList<Balance>();
        for (Amount amount : amounts) {
            Balance balance = new Balance();
            balance.setAmount(amount.getAmount());
            balance.setCurrency(amount.getCurrency().toString());
            balances.add(balance);
        }
        Mockito.when(arrayOfBalance.getBalance()).thenReturn(
                balances.toArray(new Balance[0]));
        return arrayOfBalance;
    }

    private static final HistoryInfo buildTransactionHistoryMock(
            Payment... payments) throws RemoteException {
        HistoryInfo history = Mockito.mock(HistoryInfo.class);
        List<TransactionInfo> transactions = new ArrayList<TransactionInfo>();
        for (Payment payment : payments) {
            TransactionInfo tx = OkpayProcessorTest
                    .buildTransactionInfoMock(payment);
            transactions.add(tx);
        }
        ArrayOfTransactionInfo transactionsArray = Mockito
                .mock(ArrayOfTransactionInfo.class);
        Mockito.when(transactionsArray.getTransactionInfo()).thenReturn(
                transactions.toArray(new TransactionInfo[0]));
        Mockito.when(history.getTransactions()).thenReturn(transactionsArray);
        return history;
    }

    private static final TransactionInfo buildTransactionInfoMock(
            Payment payment) throws RemoteException {
        TransactionInfo transaction = Mockito.mock(TransactionInfo.class);
        AccountInfo sender = Mockito.mock(AccountInfo.class);
        AccountInfo receiver = Mockito.mock(AccountInfo.class);
        Mockito.when(sender.getWalletID()).thenReturn(payment.getSenderId());
        Mockito.when(receiver.getWalletID())
                .thenReturn(payment.getReceiverId());
        Mockito.when(transaction.getCurrency()).thenReturn(
                payment.getAmount().getCurrency().toString());
        Mockito.when(transaction.getAmount()).thenReturn(
                payment.getAmount().getAmount());
        Mockito.when(transaction.getComment()).thenReturn(
                payment.getDescription());
        Mockito.when(transaction.getDate()).thenReturn(
                payment.getDate().toString(OKPAY_DATE_FORMAT));
        Mockito.when(transaction.getID()).thenReturn(
                Long.parseLong(payment.getId()));
        Mockito.when(transaction.getReceiver()).thenReturn(receiver);
        Mockito.when(transaction.getSender()).thenReturn(sender);
        Mockito.when(transaction.getStatus()).thenReturn(
                OperationStatus.Completed);
        return transaction;
    }
}
