package com.coinffeine.common.paymentprocessor.okpay

import scala.concurrent.duration._
import scala.concurrent.Await

import org.joda.time.DateTime
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.mockito.BDDMockito.given
import org.mockito.Matchers.{eq => the, any}
import scalaxb.Soap11Fault

import com.coinffeine.common.currency.CurrencyCode.USD
import com.coinffeine.common.paymentprocessor.okpay._

class OKPayProcessorTest extends FlatSpec with ShouldMatchers with MockitoSugar {

  val futureTimeout = 5.seconds
  val senderAccount = "OK12345"
  val receiverAccount = "OK12345"
  val token = "token"
  val transactionInfo = TransactionInfo(
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

  private trait WithOkPayProcessor {
    val fakeClient = mock[I_OkPayAPI]
    val fakeTokenGenerator = mock[TokenGenerator]
    given(fakeTokenGenerator.build(any[DateTime])).willReturn(token)
    val instance = new OKPayProcessor(fakeTokenGenerator, senderAccount, fakeClient)
  }

  "OKPayProcessor" must "be able to get the current balance" in new WithOkPayProcessor {
    val response = buildGetBalanceResponse(senderAccount, token)
    given(fakeClient.wallet_Get_Balance(
      walletID = the(Some(Some(senderAccount))),
      securityToken = the(Some(Some(token)))
    )).willReturn(response)
    val balance = Await.result(instance.currentBalance(), futureTimeout)
    balance should contain (USD(100))
  }

  it must "be able to send a payment" in new WithOkPayProcessor {
    val fakeResponse = buildSendMoneyResponse(transactionInfo)
    given(fakeClient.send_Money(
      walletID = the(Some(Some(senderAccount))),
      securityToken = the(Some(Some(token))),
      receiver = the(Some(Some(receiverAccount))),
      currency = the(Some(Some("USD"))),
      amount = the(Some(BigDecimal(100))),
      comment = the(Some(Some("comment"))),
      isReceiverPaysFees = the(Some(false)),
      invoice = the(None))).willReturn(fakeResponse)
    val response = Await.result(instance.sendPayment(receiverAccount, USD(100), "comment"), futureTimeout)
    response.paymentId should be ("250092")
  }

  it must "be able to retrieve a existing payment" in new WithOkPayProcessor {
    val fakeResponse = buildFindTransactionResponse(transactionInfo)
    given(fakeClient.transaction_Get(
      walletID = the(Some(Some(senderAccount))),
      securityToken = the(Some(Some(token))),
      txnID = the(Some(250092L)),
      invoice = the(None))).willReturn(fakeResponse)
    val response = Await.result(instance.findPayment("250092"), futureTimeout)
    response.get.paymentId should be ("250092")
  }

  private def buildSendMoneyResponse(txInfo : TransactionInfo) : Either[Soap11Fault[Any], Send_MoneyResponse] = {
    Left("errorMessage")
    Right(Send_MoneyResponse(Some(Some(txInfo))))
  }

  private def buildFindTransactionResponse(txInfo : TransactionInfo) : Either[Soap11Fault[Any], Transaction_GetResponse] = {
    Left("errorMessage")
    Right(Transaction_GetResponse(Some(Some(txInfo))))
  }

  private def buildGetBalanceResponse(walletId : String, token : String) :
  Either[Soap11Fault[Any], Wallet_Get_BalanceResponse] = {
    Left("errorMessage")
    Right(Wallet_Get_BalanceResponse(Some(Some(buildArrayOfBalance))))
  }

  private def buildArrayOfBalance : ArrayOfBalance = ArrayOfBalance(Some(Balance(Some(100),Some(Some("USD")))))


  private def buildAccountInfo(walletId : String) : AccountInfo = {
    AccountInfo(Some(1234), None, None, None, None, Some(Some(walletId)))
  }
}
