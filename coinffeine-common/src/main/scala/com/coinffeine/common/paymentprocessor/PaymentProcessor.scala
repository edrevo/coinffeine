package com.coinffeine.common.paymentprocessor

import com.coinffeine.common.currency.FiatAmount
import scala.concurrent.Future

trait PaymentProcessor {

  /** Send a payment from any of your wallets to someone.
    *
    * @param receiverId account id of receiver of payment
    * @param amount amount to send
    * @param comment to specify additional information
    * @return a Payment object containing the information of payment (receiverId and senderId
    *         properties are not provided)
    */
  def sendPayment(receiverId: String, amount: FiatAmount, comment: String): Future[Payment]

  /** Find a specific payment by id.
    *
    * @param paymentId PaymentID to search.
    * @return The payment wanted.
    */
  def findPayment(paymentId: String): Future[Option[Payment]]

  /** Returns the current balance
    *
    * @return a List of balances, one entry for each currency type
    *         amount in the account.
    */
  def currentBalance(): Future[Seq[FiatAmount]]
}
