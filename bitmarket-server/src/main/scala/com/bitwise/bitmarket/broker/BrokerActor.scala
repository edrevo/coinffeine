package com.bitwise.bitmarket.broker

import java.util.Currency
import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor.{ActorRef, Props}
import com.bitwise.bitmarket.common.protocol.OrderMatch

/** A broker actor maintains the order book of BTC trading on a given fiat currency.
  *
  * On the event of an order match it creates a child actor to manage the handshake and let the
  * parties to publish their commitments at the same time.
  */
object BrokerActor {

  trait Component {

    /** Props for creating a broker actor given some dependencies.
      *
      * @param currency  Currency to be traded for
      * @param gateway   Message gateway
      * @param handshakeArbiterProps  Props of the actor to take care of new exchange handshakes.
      *                               Will be created one per OrderMatch.
      * @param orderExpirationInterval  Time that orders take to be discarded if not renewed.
      */
    def brokerActorProps(
      currency: Currency,
      gateway: ActorRef,
      handshakeArbiterProps: OrderMatch => Props,
      orderExpirationInterval: Duration = 60 seconds): Props
  }
}
