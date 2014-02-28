package com.coinffeine.common.paymentprocessor.okpay

import scala.concurrent.duration._
import scala.concurrent.Await

import org.joda.time.DateTime
import org.mockito.BDDMockito.given
import org.mockito.Matchers.{eq => the, any}
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar

import com.coinffeine.common.currency.CurrencyCode.USD

class OKPayProcessorTest extends FlatSpec with ShouldMatchers with MockitoSugar {

  val futureTimeout = 5.seconds
  val senderAccount = "OK12345"
  val receiverAccount = "OK54321"
  val token = "token"
  val txInfo = TransactionInfo(
    Amount = Some(1),
    Comment = Some(Some("comment")),
    Currency = Some(Some("USD")),
    Date = Some(Some("2014-01-20 14:00:00")),
    Fees = Some(0),
    ID = Some(250092),
    Invoice = None,
    Net = Some(1),
    OperationName = None,
    Receiver = Some(Some(buildAccountInfo(receiverAccount))),
    Sender = Some(Some(buildAccountInfo(senderAccount))),
    Status = None
  )
  val balanceArray = ArrayOfBalance(Some(Balance(Some(100), Some(Some("USD")))))

  private trait WithOkPayProcessor {
    val fakeClient = mock[I_OkPayAPI]
    val fakeTokenGenerator = mock[TokenGenerator]
    given(fakeTokenGenerator.build(any[DateTime])).willReturn(token)
    val instance = new OKPayProcessor(fakeTokenGenerator, senderAccount, fakeClient)
  }

  "OKPayProcessor" must "be able to get the current balance" in new WithOkPayProcessor {
    given(fakeClient.wallet_Get_Balance(
      walletID = the(Some(Some(senderAccount))),
      securityToken = the(Some(Some(token)))
    )).willReturn(Right(Wallet_Get_BalanceResponse(Some(Some(balanceArray)))))
    val balance = Await.result(instance.currentBalance(), futureTimeout)
    balance should contain (USD(100))
  }

  it must "be able to send a payment" in new WithOkPayProcessor {
    given(fakeClient.send_Money(
      walletID = the(Some(Some(senderAccount))),
      securityToken = the(Some(Some(token))),
      receiver = the(Some(Some(receiverAccount))),
      currency = the(Some(Some("USD"))),
      amount = the(Some(BigDecimal(100))),
      comment = the(Some(Some("comment"))),
      isReceiverPaysFees = the(Some(false)),
      invoice = the(None))).willReturn(Right(Send_MoneyResponse(Some(Some(txInfo)))))
    val response = Await.result(instance.sendPayment(receiverAccount, USD(100), "comment"), futureTimeout)
    response.paymentId should be ("250092")
  }

  it must "be able to retrieve a existing payment" in new WithOkPayProcessor {
    given(fakeClient.transaction_Get(
      walletID = the(Some(Some(senderAccount))),
      securityToken = the(Some(Some(token))),
      txnID = the(Some(250092L)),
      invoice = the(None))).willReturn(Right(Transaction_GetResponse(Some(Some(txInfo)))))
    val response = Await.result(instance.findPayment("250092"), futureTimeout)
    response.get.paymentId should be ("250092")
  }

  def buildAccountInfo(walletId: String) =
    AccountInfo(Some(1234), None, None, None, None, Some(Some(walletId)))
}
