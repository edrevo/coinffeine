package com.coinffeine.client.api

import com.coinffeine.common.protocol.messages.brokerage.OrderMatch

sealed trait Exchange {
  def id: String
}

case class HandshakingExchange(override val id: String, orderMatch: OrderMatch) extends Exchange
case class RunningExchange(
    override val id: String, orderMatch: OrderMatch, progress: Exchange.Progress) extends Exchange
case class SuccessfulExchange(override val id: String, orderMatch: OrderMatch) extends Exchange
case class FailedExchange(override val id: String, cause: Throwable) extends Exchange

object Exchange {
  case class Progress(step: Int)
}
