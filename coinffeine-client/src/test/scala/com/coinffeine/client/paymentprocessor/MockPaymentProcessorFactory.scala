package com.coinffeine.client.paymentprocessor

import java.util.UUID

import akka.actor.{Actor, ActorRef, Props}
import org.joda.time.DateTime

import com.coinffeine.common._
import com.coinffeine.common.paymentprocessor.{AnyPayment, Payment, PaymentProcessor}

class MockPaymentProcessorFactory(initialPayments: List[AnyPayment] = List.empty) {

  @volatile var payments: List[AnyPayment] = initialPayments

  private class MockPaymentProcessor(
      fiatAddress: String,
      initialBalances: Seq[FiatAmount]) extends Actor {

    val id: String = "MockPay"

    override def receive: Receive = {
      case PaymentProcessor.Identify =>
        sender ! PaymentProcessor.Identified(id)
      case pay: PaymentProcessor.Pay[_] =>
        sendPayment(sender(), pay)
      case PaymentProcessor.FindPayment(paymentId) =>
        findPayment(sender(), paymentId)
      case PaymentProcessor.RetrieveBalance(currency) =>
        currentBalance(sender(), currency)
    }


    private def findPayment(requester: ActorRef, paymentId: String): Unit =
      payments.find(_.id == paymentId) match {
        case Some(payment) => requester ! PaymentProcessor.PaymentFound(payment)
        case None => requester ! PaymentProcessor.PaymentNotFound(paymentId)
      }

    private def currentBalance[C <: FiatCurrency](requester: ActorRef, currency: C): Unit = {
      val deltas: List[CurrencyAmount[C]] = payments.collect {
        case Payment(_, `fiatAddress`, `fiatAddress`, out: CurrencyAmount[C], _, _) => currency.Zero
        case Payment(_, _, `fiatAddress`, in: CurrencyAmount[C], _, _) => in
        case Payment(_, `fiatAddress`, _, out: CurrencyAmount[C], _, _) => -out
      }
      val initial = initialBalances.collectFirst {
        case a: CurrencyAmount[C] => a
      }.getOrElse(currency.Zero)
      val balance = deltas.foldLeft(initial)(_ + _)
      requester ! PaymentProcessor.BalanceRetrieved(balance)
    }

    private def sendPayment[C <: FiatCurrency](
        requester: ActorRef, pay: PaymentProcessor.Pay[C]): Unit =
      if (initialBalances.map(_.currency).contains(pay.amount.currency)) {
        val payment = Payment(
          UUID.randomUUID().toString,
          fiatAddress,
          pay.to,
          pay.amount,
          DateTime.now(),
          pay.comment)
        payments = payment :: payments
        requester ! PaymentProcessor.Paid(payment)
      } else {
        requester ! PaymentProcessor.PaymentFailed(
          pay, new Error("[MockPay] The user does not have an account with that currency."))
      }
  }

  def newProcessor(
      fiatAddress: String, initialBalance: Seq[FiatAmount] = Seq.empty): Props =
    Props(new MockPaymentProcessor(fiatAddress, initialBalance))
}
