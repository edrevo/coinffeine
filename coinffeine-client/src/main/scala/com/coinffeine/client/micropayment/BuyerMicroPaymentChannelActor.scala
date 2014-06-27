package com.coinffeine.client.micropayment

import scala.concurrent.Future
import scala.util.{Failure, Success}

import akka.actor._
import akka.pattern._

import com.coinffeine.client.MessageForwarding
import com.coinffeine.client.exchange.PaymentDescription
import com.coinffeine.client.micropayment.MicroPaymentChannelActor._
import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.ImmutableTransaction
import com.coinffeine.common.exchange.ExchangeProtocol
import com.coinffeine.common.exchange.MicroPaymentChannel.{FinalStep, IntermediateStep, Signatures, Step}
import com.coinffeine.common.paymentprocessor.PaymentProcessor
import com.coinffeine.common.paymentprocessor.PaymentProcessor.Paid
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.{ReceiveMessage, Subscribe}
import com.coinffeine.common.protocol.messages.exchange.{PaymentProof, StepSignatures}

/** This actor implements the buyer's side of the exchange. You can find more information about
  * the algorithm at https://github.com/Coinffeine/coinffeine/wiki/Exchange-algorithm
  */
class BuyerMicroPaymentChannelActor[C <: FiatCurrency](exchangeProtocol: ExchangeProtocol)
  extends Actor with ActorLogging with StepTimeout  {

  override def postStop(): Unit = {
    cancelTimeout()
  }

  override def receive: Receive = {
    case init: StartMicroPaymentChannel[C] =>
      new InitializedBuyer(init).startExchange()
  }

  private class InitializedBuyer(init: StartMicroPaymentChannel[C]) {
    import init._
    import init.constants.exchangeSignatureTimeout

    private val forwarding = new MessageForwarding(messageGateway, exchange, role)
    private val channel = exchangeProtocol.createProtoMicroPaymentChannel(exchange, role, deposits)
    private var lastSignedOffer: Option[ImmutableTransaction] = None

    def startExchange(): Unit = {
      subscribeToMessages()
      context.become(waitForNextStepSignature(IntermediateStep(1)))
      log.info(s"Exchange ${exchange.id}: Exchange started")
    }

    private def subscribeToMessages(): Unit = {
      val counterpart = role.her(exchange).connection
      messageGateway ! Subscribe {
        case ReceiveMessage(StepSignatures(exchange.`id`, _, _), `counterpart`) => true
        case _ => false
      }
    }

    private def withStepTimeout(step: Step)(receive: Receive): Receive = {
      scheduleStepTimeouts(exchangeSignatureTimeout)
      receive.andThen(_ => cancelTimeout()).orElse(handleTimeout(step))
    }

    private def handleTimeout(step: Step): Receive= {
      case StepSignatureTimeout =>
        val errorMsg = s"Timed out waiting for the seller to provide the signature for $step" +
          s" (out of ${exchange.parameters.breakdown.intermediateSteps}})"
        log.warning(errorMsg)
        finishWith(ExchangeFailure(TimeoutException(errorMsg), lastSignedOffer))
    }

    private val waitForFinalSignature: Receive = withStepTimeout(FinalStep) {
      waitForValidSignature(FinalStep) { signatures =>
        log.info(s"Exchange ${exchange.id}: exchange finished with success")
        // TODO: Publish transaction to blockchain
        finishWith(ExchangeSuccess)
      }
    }

    private def waitForNextStepSignature(step: IntermediateStep): Receive = withStepTimeout(step) {
      waitForValidSignature(step) { signatures =>
        lastSignedOffer = Some(channel.closingTransaction(step, signatures))
        forwarding.forwardToCounterpart(pay(step))
        context.become(nextWait(step))
      }
    }

    private def waitForValidSignature(step: Step)(body: Signatures => Unit): Receive = {

      val stepNumber = step match {
        case IntermediateStep(i) => i
        case FinalStep => exchange.parameters.breakdown.totalSteps
      }

      {
        case ReceiveMessage(StepSignatures(_, `stepNumber`, signatures), _) =>
          channel.validateStepTransactionSignatures(step, signatures) match {
            case Success(_) =>
              body(signatures)
            case Failure(cause) =>
              log.warning(s"Received invalid signature for step $step: ($signatures). Reason: $cause")
              finishWith(ExchangeFailure(
                InvalidStepSignatures(step, signatures, cause), lastSignedOffer))
          }
      }
    }

    private def finishWith(result: Any): Unit = {
      resultListeners.foreach { _ ! result }
      context.stop(self)
    }

    private def nextWait(step: IntermediateStep): Receive =
      if (step.value == exchange.parameters.breakdown.intermediateSteps) waitForFinalSignature
      else waitForNextStepSignature(IntermediateStep(step.value + 1))

    private def pay(step: IntermediateStep): Future[PaymentProof] = {
      import context.dispatcher
      implicit val timeout = PaymentProcessor.RequestTimeout

      val paymentRequest = PaymentProcessor.Pay(
        role.her(exchange).paymentProcessorAccount,
        exchange.amounts.stepFiatAmount,
        PaymentDescription(exchange.id, step)
      )
      for {
        Paid(payment) <- paymentProcessor.ask(paymentRequest).mapTo[Paid[C]]
      } yield PaymentProof(exchange.id, payment.id)
    }
  }
}

object BuyerMicroPaymentChannelActor {
  trait Component { this: ProtocolConstants.Component =>
    def micropaymentChannelActorProps[C <: FiatCurrency]: Props =
      Props[BuyerMicroPaymentChannelActor[C]]
  }
}
