package com.bitwise.bitmarket.broker

import java.util.Currency
import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor.{ActorRef, Props}

/** A broker actor maintains the order book of BTC trading on a given fiat currency. */
object BrokerActor {

  trait Component {
    def brokerActorProps(
      currency: Currency,
      gateway: ActorRef,
      orderExpirationInterval: Duration = 60 seconds): Props
  }
}
