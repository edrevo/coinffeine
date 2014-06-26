package com.coinffeine.client.micropayment

import scala.util.{Failure, Try}

import akka.actor._

import com.coinffeine.client.MessageForwarding
import com.coinffeine.client.exchange.SellerUser
import com.coinffeine.client.micropayment.MicroPaymentChannelActor._
import com.coinffeine.client.micropayment.SellerMicroPaymentChannelActor.PaymentValidationResult
import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.{ReceiveMessage, Subscribe}
import com.coinffeine.common.protocol.messages.exchange._

/** This actor implements the seller's's side of the exchange. You can find more information about
  * the algorithm at https://github.com/Coinffeine/coinffeine/wiki/Exchange-algorithm
  */
class SellerMicroPaymentChannelActor[C <: FiatCurrency]
  extends Actor with ActorLogging with Stash with StepTimeout {

  override def receive: Receive = {
    case init: StartMicroPaymentChannel[C, SellerUser[C]] => new InitializedSellerExchange(init)
  }

  private class InitializedSellerExchange(init: StartMicroPaymentChannel[C, SellerUser[C]]) {
    import init._
    import init.constants.exchangePaymentProofTimeout

    private val exchangeInfo = exchange.exchangeInfo
    private val forwarding = new MessageForwarding(
      messageGateway, exchangeInfo.counterpart.connection, exchangeInfo.broker.connection)

    messageGateway ! Subscribe {
      case ReceiveMessage(PaymentProof(exchangeInfo.`id`, _), exchangeInfo.counterpart.`connection`) => true
      case _ => false
    }
    log.info(s"Exchange ${exchangeInfo.id}: Exchange started")
    forwarding.forwardToCounterpart(StepSignatures(
      exchangeInfo.id,
      1,
      exchange.signStep(1).toTuple))
    context.become(waitForPaymentProof(1))

    private def waitForPaymentProof(step: Int): Receive = {
      scheduleStepTimeouts(exchangePaymentProofTimeout)

      {
        case ReceiveMessage(PaymentProof(_, paymentId), _) =>
          cancelTimeout()
          import context.dispatcher
          exchange.validatePayment(step, paymentId).onComplete { tryResult =>
            self ! PaymentValidationResult(tryResult)
          }
          context.become(waitForPaymentValidation(paymentId, step))
        case StepSignatureTimeout =>
          val errorMsg = "Timed out waiting for the buyer to provide a valid payment proof " +
            s"step $step (out of ${exchangeInfo.parameters.breakdown.intermediateSteps}})"
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
        if (step == exchangeInfo.parameters.breakdown.intermediateSteps) finishExchange()
        else transitionToNextStep(step)
      case _ => stash()
    }

    private def transitionToNextStep(currentStep: Int): Unit = {
      unstashAll()
      val nextStep = currentStep + 1
      forwarding.forwardToCounterpart(StepSignatures(
        exchangeInfo.id,
        nextStep,
        exchange.signStep(currentStep).toTuple))
      context.become(waitForPaymentProof(nextStep))
    }

    private def finishExchange(): Unit = {
      log.info(s"Exchange ${exchangeInfo.id}: exchange finished with success")
      forwarding.forwardToCounterpart(StepSignatures(
        exchangeInfo.id,
        exchangeInfo.parameters.breakdown.totalSteps,
        exchange.finalSignatures.toTuple))
      finishWith(ExchangeSuccess)
    }

    private def finishWith(result: Any): Unit = {
      resultListeners.foreach { _ ! result }
      context.stop(self)
    }
  }
}

object SellerMicroPaymentChannelActor {
  private case class PaymentValidationResult(result: Try[Unit])

  trait Component { this: ProtocolConstants.Component =>
    def exchangeActorProps[C <: FiatCurrency]: Props = Props[SellerMicroPaymentChannelActor[C]]
  }
}
