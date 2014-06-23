package com.coinffeine.client.micropayment

import scala.concurrent.duration.FiniteDuration

import akka.actor.{Actor, Cancellable}

import com.coinffeine.client.micropayment.MicroPaymentChannelActor.StepSignatureTimeout

private[micropayment] trait StepTimeout { this: Actor =>

  private var stepTimeout: Option[Cancellable] = None

  protected def scheduleStepTimeouts(delay: FiniteDuration): Unit = {
    import context.dispatcher
    stepTimeout = Some(context.system.scheduler.scheduleOnce(
        delay = delay,
        receiver = self,
        message = StepSignatureTimeout
    ))
  }

  protected def cancelTimeout(): Unit = stepTimeout.map(_.cancel())
}
