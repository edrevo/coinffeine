package com.coinffeine.client.paymentprocessor

import java.util.UUID
import scala.concurrent.Future

import org.joda.time.DateTime

import com.coinffeine.common.paymentprocessor.{Payment, PaymentProcessor}
import com.coinffeine.common.currency.FiatAmount

class MockPaymentProcessor(
    fiatAddress: String,
    initialBalance: Seq[FiatAmount]= Seq.empty,
    initialPayments: List[Payment] = List.empty) extends PaymentProcessor {

  @volatile var payments: List[Payment] = initialPayments

  override def id: String = "MockPay"

  override def findPayment(paymentId: String): Future[Option[Payment]] =
    Future.successful(payments.find(_.id == paymentId))

  override def currentBalance(): Future[Seq[FiatAmount]] = Future.successful {
    val paymentGroups = payments
      .filter(p => p.receiverId == fiatAddress || p.senderId == fiatAddress)
      .groupBy(_.amount.currency)
    initialBalance.map { balance =>
      paymentGroups.getOrElse(balance.currency, List.empty).collect {
        case Payment(_, `fiatAddress`, _, amount, _, _) => -amount
        case Payment(_, _, `fiatAddress`, amount, _, _) => amount
      }.foldLeft(balance)(_ + _)
    }
  }

  override def sendPayment(
      receiverId: String,
      amount: FiatAmount,
      comment: String): Future[Payment] =
    if (initialBalance.map(_.currency).contains(amount.currency)) {
      Future.successful {
        val payment = Payment(
          UUID.randomUUID().toString,
          fiatAddress,
          receiverId,
          amount,
          DateTime.now(),
          comment)
        payments = payment :: payments
        payment
      }
    } else {
      Future.failed(new Error("[MockPay] The user does not have an account with that currency."))
    }
}
