package com.coinffeine.common.paymentprocessor.okpay

import java.util.Currency
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormat
import scalaxb.{DispatchHttpClients, Soap11Clients, Soap11Fault}

import com.coinffeine.common.currency.FiatAmount
import com.coinffeine.common.paymentprocessor.{Payment, PaymentProcessor, PaymentProcessorException}
import com.coinffeine.common.paymentprocessor.okpay.generated._

class OKPayProcessor(
    tokenGenerator: TokenGenerator,
    account: String,
    service: I_OkPayAPI = OKPayProcessor.defaultClient()) extends PaymentProcessor {

  /** Send a payment from any of your wallets to someone wallet.
    *
    * @param receiverId OKPay Wallet ID of receiver of payment, example: OK321345.
    * @param amount amount to send.
    * @return a Payment Object.
    */
  override def sendPayment(receiverId: String, amount: FiatAmount, comment: String): Future[Payment] =
    Future {
      val response = getResponse(service.send_Money(
        walletID = Some(Some(account)),
        securityToken = Some(Some(buildCurrentToken())),
        receiver = Some(Some(receiverId)),
        currency = Some(Some(amount.currency.getCurrencyCode)),
        amount = Some(amount.amount),
        comment = Some(Some(comment)),
        isReceiverPaysFees = Some(false),
        invoice = None
      ))
      response.Send_MoneyResult.flatten.flatMap(parseTransactionInfo)
        .getOrElse(throw new PaymentProcessorException("Cannot parse the sent payment: " + response))
    }

  /** Find an specific payment by id.
    *
    * Note: The date attribute on Payment is in the Timezone defined by the user in OKPay.
    * Please, use the payment date with caution.
    *
    * @param paymentId PaymentId to search. Example: 2205909.
    * @return The payment wanted.
    */
  override def findPayment(paymentId: String): Future[Option[Payment]] = Future {
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
  override def currentBalance(): Future[Seq[FiatAmount]] = Future {
    val token: String = buildCurrentToken()
    val response = getResponse(service.wallet_Get_Balance(
      walletID = Some(Some(this.account)),
      securityToken = Some(Some(token))
    ))
    response.Wallet_Get_BalanceResult.flatten.map(parseArrayOfBalance)
      .getOrElse(throw new PaymentProcessorException("Cannot parse balances: " + response))
  }

  private def parseTransactionInfo(txInfo: TransactionInfo): Option[Payment] = {
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
        val currency = Currency.getInstance(txInfo.Currency.get.get)
        val amount = FiatAmount(net, currency)
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

  private def parseArrayOfBalance(balances: ArrayOfBalance): Seq[FiatAmount] =
    balances.Balance.map(balance =>
      FiatAmount(balance.get.Amount.get, Currency.getInstance(balance.get.Currency.get.get))
    )

  private def buildCurrentToken() = tokenGenerator.build(DateTime.now(DateTimeZone.UTC))
}

private[this] object OKPayProcessor {

  private val DateFormat = "yyyy-MM-dd HH:mm:ss"

  private def defaultClient() =
    new BasicHttpBinding_I_OkPayAPIBindings with Soap11Clients with DispatchHttpClients {}.service
}
