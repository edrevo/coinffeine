package com.coinffeine.client.exchange

import akka.actor.{ActorRef, Props}

/** An exchange actor is in charge of performing each of the exchange steps by sending/receiving
  * bitcoins and fiat.
  */
object ExchangeActor {

  /** Sent to the the actor to start the actual exchange. */
  case class StartExchange(
      messageGateway: ActorRef,
      resultListeners: Set[ActorRef]
  )

  /** Sent to the exchange listeners to notify success of the exchange */
  case object ExchangeSuccess

  trait Component {
    /** Create the properties of an exchange actor.
      *
      * @param exchange          Class that contains the exchange logic
      * @return                  Actor properties
      */
    def exchangeActorProps(exchange: Exchange): Props
  }
}
