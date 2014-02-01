package com.bitwise.bitmarket.broker

import java.util.Currency
import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor.Props

import com.bitwise.bitmarket.common.PeerConnection
import com.bitwise.bitmarket.common.protocol.{OrderMatch, Order, Quote}

/** A broker actor maintains the order book of BTC trading on a given fiat currency.
  *
  * These are the messages produced and consumed by such a broker.
  */
object BrokerActor {

  case object QuoteRequest
  case class QuoteResponse(quote: Quote)
  case class OrderPlacement(order: Order)
  case class OrderCancellation(requester: PeerConnection)
  case class NotifyCross(cross: OrderMatch)

  trait Component {
    def brokerActorProps(currency: Currency, orderExpirationInterval: Duration = 60 seconds): Props
  }
}
