package com.coinffeine.client.micropayment

import scala.concurrent.Future
import scala.util.{Failure, Try}

import akka.actor._
import akka.pattern._

import com.coinffeine.client.MessageForwarding
import com.coinffeine.client.exchange.PaymentDescription
import com.coinffeine.client.micropayment.MicroPaymentChannelActor._
import com.coinffeine.client.micropayment.SellerMicroPaymentChannelActor.PaymentValidationResult
import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.paymentprocessor.PaymentProcessor
import com.coinffeine.common.paymentprocessor.PaymentProcessor.PaymentFound
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.{ReceiveMessage, Subscribe}
import com.coinffeine.common.protocol.messages.exchange._
import com.coinffeine.common.protocol.protobuf.CoinffeineProtobuf.FiatAmount

/** This actor implements the seller's's side of the exchange. You can find more information about
  * the algorithm at https://github.com/Coinffeine/coinffeine/wiki/Exchange-algorithm
  */
class SellerMicroPaymentChannelActor[C <: FiatCurrency]
  extends Actor with ActorLogging with Stash with StepTimeout {

  import context.dispatcher

  override def receive: Receive = {
    case init: StartMicroPaymentChannel[C] => new InitializedSellerExchange(init)
  }

  private class InitializedSellerExchange(init: StartMicroPaymentChannel[C]) {
    import init._
    import init.constants.exchangePaymentProofTimeout

    private val forwarding = new MessageForwarding(messageGateway, exchange, role)

    private val counterpart = role.her(exchange).connection
    messageGateway ! Subscribe {
      case ReceiveMessage(PaymentProof(exchange.`id`, _), `counterpart`) => true
      case _ => false
    }
    log.info(s"Exchange ${exchange.id}: Exchange started")
    forwarding.forwardToCounterpart(StepSignatures(
      exchange.id,
      1,
      channel.signStep(1).toTuple))
    context.become(waitForPaymentProof(1))

    private def waitForPaymentProof(step: Int): Receive = {
      scheduleStepTimeouts(exchangePaymentProofTimeout)

      {
        case ReceiveMessage(PaymentProof(_, paymentId), _) =>
          cancelTimeout()
          validatePayment(step, paymentId).onComplete { tryResult =>
            self ! PaymentValidationResult(tryResult)
          }
          context.become(waitForPaymentValidation(paymentId, step))
        case StepSignatureTimeout =>
          val errorMsg = "Timed out waiting for the buyer to provide a valid payment proof " +
            s"step $step (out of ${exchange.parameters.breakdown.intermediateSteps}})"
          log.warning(errorMsg)
          finishWith(ExchangeFailure(TimeoutException(errorMsg), lastOffer = None))
      }
    }

    private def waitForPaymentValidation(paymentId: String, step: Int): Receive = {
      case PaymentValidationResult(Failure(cause)) =>
        unstashAll()
        log.warning(s"Invalid payment proof received in step $step: $paymentId. Reason: $cause")
        context.become(waitForPaymentProof(step))
      case PaymentValidationResult(_) =>
        if (step == exchange.parameters.breakdown.intermediateSteps) finishExchange()
        else transitionToNextStep(step)
      case _ => stash()
    }

    private def transitionToNextStep(currentStep: Int): Unit = {
      unstashAll()
      val nextStep = currentStep + 1
      forwarding.forwardToCounterpart(StepSignatures(
        exchange.id,
        nextStep,
        channel.signStep(currentStep).toTuple
      ))
      context.become(waitForPaymentProof(nextStep))
    }

    private def finishExchange(): Unit = {
      log.info(s"Exchange ${exchange.id}: exchange finished with success")
      forwarding.forwardToCounterpart(StepSignatures(
        exchange.id,
        exchange.parameters.breakdown.totalSteps,
        channel.finalSignatures.toTuple
      ))
      finishWith(ExchangeSuccess)
    }

    private def finishWith(result: Any): Unit = {
      resultListeners.foreach { _ ! result }
      context.stop(self)
    }

    private def validatePayment(step: Int, paymentId: String): Future[Unit] = {
      implicit val timeout = PaymentProcessor.RequestTimeout
      for {
        PaymentFound(payment) <- paymentProcessor
          .ask(PaymentProcessor.FindPayment(paymentId)).mapTo[PaymentFound[C]]
      } yield {
        require(payment.amount == exchange.amounts.stepFiatAmount,
          s"Payment $step amount does not match expected amount")
        require(payment.receiverId == exchange.seller.paymentProcessorAccount,
          s"Payment $step is not being sent to the seller")
        require(payment.senderId == exchange.buyer.paymentProcessorAccount,
          s"Payment $step is not coming from the buyer")
        require(payment.description == PaymentDescription(exchange.id, step),
          s"Payment $step does not have the required description")
      }
    }
  }
}

object SellerMicroPaymentChannelActor {
  private case class PaymentValidationResult(result: Try[Unit])

  trait Component { this: ProtocolConstants.Component =>
    def exchangeActorProps[C <: FiatCurrency]: Props = Props[SellerMicroPaymentChannelActor[C]]
  }
}
