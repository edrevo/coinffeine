package com.coinffeine.common.paymentprocessor

import akka.actor.Props

import com.coinffeine.common.{CurrencyAmount, FiatCurrency}

object PaymentProcessor {

  /** The ID of the payment processor. */
  type Id = String

  /** The ID type of a user account in the payment processor. */
  type AccountId = String

  /** The credentials of a user account in the payment processor. */
  type AccountCredentials = String

  /** The ID type of a payment registered by the payment processor. */
  type PaymentId = String

  /** A message sent to the payment processor in order to identify itself. */
  case object Identify

  /** A message sent by the payment processor identifying itself. */
  case class Identified(id: Id)

  /** A message sent to the payment processor ordering a new pay.
    *
    * @param to The ID of the receiver account
    * @param amount The amount of fiat currency to pay
    * @param comment The comment to be attached to the payment
    * @tparam C The fiat currency of the payment amount
    */
  case class Pay[C <: FiatCurrency](to: AccountId,
                                    amount: CurrencyAmount[C],
                                    comment: String)

  /** A message sent by the payment process in order to notify of a successful payment. */
  case class Paid[C <: FiatCurrency](payment: Payment[C])

  /** A message sent by the payment processor to notify a payment failure.
    *
    * @param request The original pay message that cannot be processed.
    * @param error The error that prevented the request to be processed
    * @tparam C The fiat currency of the payment amount
    */
  case class PaymentFailed[C <: FiatCurrency](request: Pay[C], error: Throwable)

  /** A message sent to the payment processor in order to find a payment. */
  case class FindPayment(payment: PaymentId)

  /** A message sent by the payment processor to notify a found payment. */
  case class PaymentFound[C <: FiatCurrency](payment: Payment[C])

  /** A message sent by the payment processor to notify a not found payment. */
  case class PaymentNotFound(payment: PaymentId)

  /** A message sent to the payment processor to retrieve the current balance
    * in the given currency.
    * */
  case class RetrieveBalance[C <: FiatCurrency](currency: C)

  /** A message sent by the payment processor reporting the current balance in the
    * given currency.
    * */
  case class BalanceRetrieved[C <: FiatCurrency](balance: CurrencyAmount[C])

  /** A message sent by the payment processor reporting that the current balance in the
    * given currency cannot be retrieved.
    */
  case class BalanceRetrievalFailed[C <: FiatCurrency](currency: C, error: Throwable)

  /** A component able to provide the Akka properties needed to instantiate a new
    * Payment processor actor.
    */
  trait Component {
    def paymentProcessorProps(account: AccountId, credentials: AccountCredentials): Props
  }
}
