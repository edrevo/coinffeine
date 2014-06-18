package com.coinffeine.common.paymentprocessor.okpay

import scala.concurrent.duration._

import akka.actor.Props
import org.joda.time.DateTime
import org.mockito.BDDMockito.given
import org.mockito.Matchers.any
import org.scalatest.mock.MockitoSugar

import com.coinffeine.common.{AkkaSpec, Currency}
import com.coinffeine.common.paymentprocessor.{Payment, PaymentProcessor}
import com.coinffeine.common.paymentprocessor.okpay.generated._

class OKPayProcessorTest extends AkkaSpec("OkPayTest") with MockitoSugar {

  val futureTimeout = 5.seconds
  val senderAccount = "OK12345"
  val receiverAccount = "OK54321"
  val token = "token"
  val amount = Currency.UsDollar(100)
  val txInfo = TransactionInfo(
    Amount = Some(amount.value),
    Comment = Some(Some("comment")),
    Currency = Some(Some(amount.currency.javaCurrency.getCurrencyCode)),
    Date = Some(Some("2014-01-20 14:00:00")),
    Fees = Some(0),
    ID = Some(250092),
    Invoice = None,
    Net = Some(amount.value),
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
    val processor = system.actorOf(Props(new OKPayProcessor(
      account = senderAccount,
      client = new OKPayClient {
        override def service: I_OkPayAPI = fakeClient
      },
      tokenGenerator = fakeTokenGenerator
    )))
  }

  "OKPayProcessor" must "identify itself" in new WithOkPayProcessor {
    processor ! PaymentProcessor.Identify
    expectMsg(PaymentProcessor.Identified("OKPAY"))
  }

  it must "be able to get the current balance" in new WithOkPayProcessor {
    given(fakeClient.wallet_Get_Balance(
      walletID = Some(Some(senderAccount)),
      securityToken = Some(Some(token))
    )).willReturn(Right(Wallet_Get_BalanceResponse(Some(Some(balanceArray)))))
    processor ! PaymentProcessor.RetrieveBalance(Currency.UsDollar)
    expectMsg(PaymentProcessor.BalanceRetrieved(`amount`))
  }

  it must "be able to send a payment" in new WithOkPayProcessor {
    given(fakeClient.send_Money(
      walletID = Some(Some(senderAccount)),
      securityToken = Some(Some(token)),
      receiver = Some(Some(receiverAccount)),
      currency = Some(Some("USD")),
      amount = Some(amount.value),
      comment = Some(Some("comment")),
      isReceiverPaysFees = Some(false),
      invoice = None)).willReturn(Right(Send_MoneyResponse(Some(Some(txInfo)))))
    processor ! PaymentProcessor.Pay(receiverAccount, amount, "comment")
    expectMsgPF() {
      case PaymentProcessor.Paid(Payment(
        "250092", `senderAccount`, `receiverAccount`, `amount`, _, "comment")) => ()
    }
  }

  it must "be able to retrieve a existing payment" in new WithOkPayProcessor {
    given(fakeClient.transaction_Get(
      walletID = Some(Some(senderAccount)),
      securityToken = Some(Some(token)),
      txnID = Some(250092L),
      invoice = None)).willReturn(Right(Transaction_GetResponse(Some(Some(txInfo)))))
    processor ! PaymentProcessor.FindPayment("250092")
    expectMsgPF() {
      case PaymentProcessor.PaymentFound(Payment(
      "250092", `senderAccount`, `receiverAccount`, `amount`, _, "comment")) => ()
    }
  }

  def buildAccountInfo(walletId: String) =
    AccountInfo(Some(1234), None, None, None, None, Some(Some(walletId)))
}
