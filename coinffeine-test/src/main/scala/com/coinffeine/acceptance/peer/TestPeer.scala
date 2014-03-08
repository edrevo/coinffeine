package com.coinffeine.acceptance.peer

import java.io.Closeable
import java.util.Currency
import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor._
import akka.pattern._
import akka.util.Timeout

import com.coinffeine.common.protocol.Order
import com.coinffeine.common.protocol.messages.brokerage.{QuoteRequest, Quote}

/** Testing façade for a Coinffeine peer. */
class TestPeer(peerActorProps: Props) extends Closeable {
  implicit private val timeout = Timeout(1.minute)
  private val system = ActorSystem()
  private val peerRef = system.actorOf(peerActorProps)

  def askForQuote(currency: Currency): Future[Quote] =
    (peerRef ? QuoteRequest(currency)).mapTo[Quote]

  def placeOrder(order: Order): Unit = {}

  def close(): Unit = { system.shutdown() }
}
