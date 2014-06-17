package com.coinffeine.common.paymentprocessor

import scala.concurrent.Future

import com.coinffeine.common.{CurrencyAmount, FiatCurrency}

trait PaymentProcessor {

  type AccountId = String

  /** Unique identifier of this payment processor */
  def id: AccountId

  /** Send a payment from any of your wallets to someone.
    *
    * @param receiverId account id of receiver of payment
    * @param amount amount to send
    * @param comment to specify additional information
    * @tparam C The currency the payment is specified
    * @return a Payment object containing the information of payment (receiverId and senderId
    *         properties are not provided)
    */
  def sendPayment[C <: FiatCurrency](receiverId: AccountId, amount: CurrencyAmount[C], comment: String): Future[Payment[C]]

  /** Find a specific payment by id.
    *
    * @param paymentId PaymentID to search.
    * @return The payment wanted.
    */
  def findPayment(paymentId: String): Future[Option[AnyPayment]]

  /** Returns the current balance in the given currency.
    *
    * @return The current balance for currency C
    */
  def currentBalance[C <: FiatCurrency](currency: C): Future[CurrencyAmount[C]]
}
