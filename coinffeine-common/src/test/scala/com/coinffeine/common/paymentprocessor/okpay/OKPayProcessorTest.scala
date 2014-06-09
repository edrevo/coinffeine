package com.coinffeine.common.paymentprocessor.okpay

import scala.concurrent.Await
import scala.concurrent.duration._

import org.joda.time.DateTime
import org.mockito.BDDMockito.given
import org.mockito.Matchers.any
import org.scalatest.mock.MockitoSugar

import com.coinffeine.common.{Currency, UnitTest}
import com.coinffeine.common.paymentprocessor.okpay.generated._

class OKPayProcessorTest extends UnitTest with MockitoSugar {

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
      walletID = Some(Some(senderAccount)),
      securityToken = Some(Some(token))
    )).willReturn(Right(Wallet_Get_BalanceResponse(Some(Some(balanceArray)))))
    val balance = Await.result(instance.currentBalance(Currency.UsDollar), futureTimeout)
    balance should be (Currency.UsDollar(100))
  }

  it must "be able to send a payment" in new WithOkPayProcessor {
    given(fakeClient.send_Money(
      walletID = Some(Some(senderAccount)),
      securityToken = Some(Some(token)),
      receiver = Some(Some(receiverAccount)),
      currency = Some(Some("USD")),
      amount = Some(BigDecimal(100)),
      comment = Some(Some("comment")),
      isReceiverPaysFees = Some(false),
      invoice = None)).willReturn(Right(Send_MoneyResponse(Some(Some(txInfo)))))
    val futureResponse = instance.sendPayment(receiverAccount, Currency.UsDollar(100), "comment")
    val response = Await.result(futureResponse, futureTimeout)
    response.id should be ("250092")
  }

  it must "be able to retrieve a existing payment" in new WithOkPayProcessor {
    given(fakeClient.transaction_Get(
      walletID = Some(Some(senderAccount)),
      securityToken = Some(Some(token)),
      txnID = Some(250092L),
      invoice = None)).willReturn(Right(Transaction_GetResponse(Some(Some(txInfo)))))
    val response = Await.result(instance.findPayment("250092"), futureTimeout)
    response.get.id should be ("250092")
  }

  def buildAccountInfo(walletId: String) =
    AccountInfo(Some(1234), None, None, None, None, Some(Some(walletId)))
}
