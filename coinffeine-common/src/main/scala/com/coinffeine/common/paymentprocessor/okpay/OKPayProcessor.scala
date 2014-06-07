package com.coinffeine.common.paymentprocessor.okpay

import java.util.{Currency => JavaCurrency}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormat
import scalaxb.{DispatchHttpClients, Soap11Clients, Soap11Fault}

import com.coinffeine.common._
import com.coinffeine.common.paymentprocessor.{AnyPayment, Payment, PaymentProcessor, PaymentProcessorException}
import com.coinffeine.common.paymentprocessor.okpay.generated._

class OKPayProcessor(
    tokenGenerator: TokenGenerator,
    account: String,
    service: I_OkPayAPI = OKPayProcessor.defaultClient()) extends PaymentProcessor {

  override def id: String = OKPayProcessor.Id

  /** Send a payment from any of your wallets to someone wallet.
    *
    * @param receiverId OKPay Wallet ID of receiver of payment, example: OK321345.
    * @param amount amount to send.
    * @return a Payment Object.
    */
  override def sendPayment[C <: FiatCurrency](receiverId: String,
                                              amount: CurrencyAmount[C],
                                              comment: String): Future[Payment[C]] = {
    Future {
      val response = getResponse(service.send_Money(
        walletID = Some(Some(account)),
        securityToken = Some(Some(buildCurrentToken())),
        receiver = Some(Some(receiverId)),
        currency = Some(Some(amount.currency.javaCurrency.getCurrencyCode)),
        amount = Some(amount.value),
        comment = Some(Some(comment)),
        isReceiverPaysFees = Some(false),
        invoice = None
      ))
      val payment = response
        .Send_MoneyResult
        .flatten
        .flatMap(parseTransactionInfo)
        .getOrElse(throw new PaymentProcessorException("Cannot parse the sent payment: " + response))
      val expectedCurrency = amount.currency
      val actualCurrency = payment.amount.currency
      if (actualCurrency != expectedCurrency) throw new PaymentProcessorException(
        s"payment returned by OKPay is expressed in $actualCurrency, but $expectedCurrency was expected")
      else payment.asInstanceOf[Payment[C]]
    }
  }

  /** Find an specific payment by id.
    *
    * Note: The date attribute on Payment is in the Timezone defined by the user in OKPay.
    * Please, use the payment date with caution.
    *
    * @param paymentId PaymentId to search. Example: 2205909.
    * @return The payment wanted.
    */
  override def findPayment(paymentId: String): Future[Option[AnyPayment]] = Future {
    getResponse(service.transaction_Get(
      walletID = Some(Some(this.account)),
      securityToken = Some(Some(buildCurrentToken())),
      txnID = Some(paymentId.toLong),
      invoice = None)
    ).Transaction_GetResult.flatten.flatMap(parseTransactionInfo)
  }

  /** Returns the account balance.
    *
    * The balance is a Sequence of FiatAmount, one FiatAmount
    * by each distinct currency on the wallet.
    * Example: 20 EUR, 50 GBP and 100 USD.
    *
    * @return a Sequence of FiatAmount.
    */
  override def currentBalance[C <: FiatCurrency](currency: C): Future[CurrencyAmount[C]] = Future {
    val token: String = buildCurrentToken()
    val response = getResponse(service.wallet_Get_Balance(
      walletID = Some(Some(this.account)),
      securityToken = Some(Some(token))
    ))
    response.Wallet_Get_BalanceResult.flatten.map(b => parseArrayOfBalance(b, currency))
      .getOrElse(throw new PaymentProcessorException("Cannot parse balances: " + response))
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

  private val DateFormat = "yyyy-MM-dd HH:mm:ss"

  private def defaultClient() =
    new BasicHttpBinding_I_OkPayAPIBindings with Soap11Clients with DispatchHttpClients {}.service
}
