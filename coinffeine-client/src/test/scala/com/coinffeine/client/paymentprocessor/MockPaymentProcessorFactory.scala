package com.coinffeine.client.paymentprocessor

import java.util.UUID
import scala.concurrent.Future

import org.joda.time.DateTime

import com.coinffeine.common._
import com.coinffeine.common.paymentprocessor.{AnyPayment, Payment, PaymentProcessor}
import com.coinffeine.common.paymentprocessor.Payment

class MockPaymentProcessorFactory(initialPayments: List[AnyPayment] = List.empty) {

  @volatile var payments: List[AnyPayment] = initialPayments

  private class MockPaymentProcessor(
      fiatAddress: String,
      initialBalances: Seq[AnyFiatCurrencyAmount]) extends PaymentProcessor {

    override def id: String = "MockPay"

    override def findPayment(paymentId: String): Future[Option[AnyPayment]] =
      Future.successful(payments.find(_.id == paymentId))

    override def currentBalance[C <: Currency](currency: C): Future[currency.Amount] = Future.successful {
      val deltas = payments.collect {
        case Payment(_, `fiatAddress`, `fiatAddress`, out: currency.Amount, _, _) => currency.Amount.Zero
        case Payment(_, _, `fiatAddress`, in: currency.Amount, _, _) => in
        case Payment(_, `fiatAddress`, _, out: currency.Amount, _, _) => -out
      }
      val initial = initialBalances.collectFirst {
        case a: currency.Amount => a
      }.getOrElse(currency.Amount.Zero)
      deltas.foldLeft(initial)(_ + _)
    }

    override def sendPayment[C <: FiatCurrency](receiverId: String,
                                            amount: CurrencyAmount[C],
                                            comment: String): Future[Payment[C]] =
      if (initialBalances.map(_.currency).contains(amount.currency)) {
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

  def newProcessor(
      fiatAddress: String, initialBalance: Seq[AnyFiatCurrencyAmount] = Seq.empty): PaymentProcessor =
    new MockPaymentProcessor(fiatAddress, initialBalance)
}
