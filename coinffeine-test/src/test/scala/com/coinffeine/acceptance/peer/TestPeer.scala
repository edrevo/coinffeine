package com.coinffeine.acceptance.peer

import java.util.Currency
import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor._
import akka.pattern._
import akka.util.Timeout

import com.coinffeine.acceptance.TestActorSystem
import com.coinffeine.common.protocol.messages.brokerage.{Order, Quote, QuoteRequest}

/** Testing façade for a Coinffeine peer. */
class TestPeer(peerSupervisorProps: Props, name: String)
    extends TestActorSystem(peerSupervisorProps, name) {
  implicit private val timeout = Timeout(3, SECONDS)

  def askForQuote(currency: Currency): Future[Quote] =
    (supervisorRef ? QuoteRequest(currency)).mapTo[Quote]

  def placeOrder(order: Order): Unit = {}
}
