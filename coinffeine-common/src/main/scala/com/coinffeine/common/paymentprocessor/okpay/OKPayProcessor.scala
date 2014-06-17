package com.coinffeine.common.paymentprocessor.okpay

import java.util.{Currency => JavaCurrency}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

import akka.actor.{Actor, ActorRef, Props}
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormat
import scalaxb.Soap11Fault

import com.coinffeine.common._
import com.coinffeine.common.paymentprocessor.{AnyPayment, Payment, PaymentProcessor, PaymentProcessorException}
import com.coinffeine.common.paymentprocessor.okpay.generated._

class OKPayProcessor(
    account: String,
    client: OKPayClient,
    tokenGenerator: TokenGenerator) extends Actor {

  private val service = client.service

  override def receive: Receive = {
    case PaymentProcessor.Identify =>
      sender ! PaymentProcessor.Identified(OKPayProcessor.Id)
    case pay: PaymentProcessor.Pay[_] =>
      sendPayment(sender(), pay)
    case PaymentProcessor.FindPayment(paymentId) =>
      findPayment(sender(), paymentId)
    case PaymentProcessor.RetrieveBalance(currency) =>
      currentBalance(sender(), currency)
  }

  private def sendPayment[C <: FiatCurrency](requester: ActorRef,
                                             pay: PaymentProcessor.Pay[C]): Unit = {
    Future {
      val response = getResponse(service.send_Money(
        walletID = Some(Some(account)),
        securityToken = Some(Some(buildCurrentToken())),
        receiver = Some(Some(pay.to)),
        currency = Some(Some(pay.amount.currency.javaCurrency.getCurrencyCode)),
        amount = Some(pay.amount.value),
        comment = Some(Some(pay.comment)),
        isReceiverPaysFees = Some(false),
        invoice = None
      ))
      val payment = response
        .Send_MoneyResult
        .flatten
        .flatMap(parseTransactionInfo)
        .getOrElse(throw new PaymentProcessorException("Cannot parse the sent payment: " + response))
      val expectedCurrency = pay.amount.currency
      val actualCurrency = payment.amount.currency
      if (actualCurrency != expectedCurrency) throw new PaymentProcessorException(
        s"payment is expressed in $actualCurrency, but $expectedCurrency was expected")
      else payment.asInstanceOf[Payment[C]]
    }.onComplete {
      case Success(payment) =>
        requester ! PaymentProcessor.Paid(payment)
      case Failure(error) =>
        requester ! PaymentProcessor.PaymentFailed(pay, error)
    }
  }

  private def findPayment(requester: ActorRef, paymentId: String): Unit = Future {
    getResponse(service.transaction_Get(
      walletID = Some(Some(this.account)),
      securityToken = Some(Some(buildCurrentToken())),
      txnID = Some(paymentId.toLong),
      invoice = None)
    ).Transaction_GetResult.flatten.flatMap(parseTransactionInfo)
  }.onComplete {
    case Success(Some(payment)) => requester ! PaymentProcessor.PaymentFound(payment)
    case Success(None) => requester ! PaymentProcessor.PaymentNotFound(paymentId)
    case Failure(error) => throw error
  }

  private def currentBalance[C <: FiatCurrency](requester: ActorRef,
                                                currency: C): Unit = Future {
    val token: String = buildCurrentToken()
    val response = getResponse(service.wallet_Get_Balance(
      walletID = Some(Some(this.account)),
      securityToken = Some(Some(token))
    ))
    response.Wallet_Get_BalanceResult.flatten.map(b => parseArrayOfBalance(b, currency))
      .getOrElse(throw new PaymentProcessorException("Cannot parse balances: " + response))
  }.onComplete {
    case Success(balance) => requester ! PaymentProcessor.BalanceRetrieved(balance)
    case Failure(error) => requester ! PaymentProcessor.BalanceRetrievalFailed(currency, error)
  }

  private def parseTransactionInfo(txInfo: TransactionInfo): Option[AnyPayment] = {
    txInfo match {
      case TransactionInfo(
        Some(amount),
        Flatten(description),
        Flatten(rawCurrency),
        Flatten(rawDate),
        _,
        Some(paymentId),
        _,
        Some(net),
        _,
        Flatten(WalletId(receiverId)),
        Flatten(WalletId(senderId)),
        _
      ) =>
        val currency = FiatCurrency(JavaCurrency.getInstance(txInfo.Currency.get.get))
        val amount = currency.amount(net)
        val date = DateTimeFormat.forPattern(OKPayProcessor.DateFormat).parseDateTime(rawDate)
        Some(Payment(paymentId.toString, senderId, receiverId, amount, date, description))
      case _ => None
    }
  }

  private object WalletId {
    def unapply(info: AccountInfo): Option[String] = info.WalletID.flatten
  }

  private object Flatten {
    def unapply[T](option: Option[Option[T]]): Option[T] = option.flatten
  }

  private def getResponse[T](response: Either[Soap11Fault[Any], T]): T = response.fold(
    msg => throw new PaymentProcessorException("Error when sending the payment: "  + msg),
    identity
  )

  private def parseArrayOfBalance[C <: FiatCurrency](
      balances: ArrayOfBalance, expectedCurrency: C): CurrencyAmount[C] = {
    val amounts = balances.Balance.collect {
      case Some(Balance(a, c)) if c.get.get == expectedCurrency.javaCurrency.getCurrencyCode => a.get
    }
    expectedCurrency.amount(amounts.sum)
  }

  private def buildCurrentToken() = tokenGenerator.build(DateTime.now(DateTimeZone.UTC))

}

object OKPayProcessor {

  val Id = "OKPAY"

  trait Component extends PaymentProcessor.Component {

    this: TokenGenerator.Component with OKPayClient.Component =>

    override def paymentProcessorProps(account: PaymentProcessor.AccountId,
                                       credentials: PaymentProcessor.AccountCredentials): Props =
      Props(new OKPayProcessor(account, okPayClient, createTokenGenerator(credentials)))
  }

  private val DateFormat = "yyyy-MM-dd HH:mm:ss"
}
