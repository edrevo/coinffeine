package com.coinffeine.client.exchange

import scala.util.Try

import akka.actor.{Props, ActorRef}

import com.coinffeine.client.ExchangeInfo
import com.coinffeine.common.protocol.serialization.TransactionSerialization

/** Am exchange actor is in charge of performing each of the exchange steps by sending/receiving
  * bitcoins and fiat
  */
object ExchangeActor {
  /** Sent to the exchange listeners to notify success of the exchange */
  case object ExchangeSuccess

  trait Component {
    /** Create the properties of an exchange actor.
      *
      * @param exchangeInfo             Information about the exchange
      * @param exchange                 Class that contains the exchange logic
      * @param messageGateway           Communications gateway
      * @param transactionSerialization Serialization method for transactions
      * @param resultListeners          Actors to be notified of the handshake result
      * @return                         Actor properties
      */
    def exchangeActorProps(
      exchangeInfo: ExchangeInfo,
      exchange: Exchange,
      messageGateway: ActorRef,
      transactionSerialization: TransactionSerialization,
      resultListeners: Seq[ActorRef]): Props
  }
}
