package com.coinffeine.client.app

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

import akka.actor.ActorRef
import akka.pattern._
import akka.util.Timeout

import com.coinffeine.client.api.{CoinffeineNetwork, Exchange}
import com.coinffeine.client.api.CoinffeineNetwork._
import com.coinffeine.client.peer.PeerActor
import com.coinffeine.client.peer.PeerActor.{CancelOrder, OpenOrder}
import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.protocol.messages.brokerage._

class DefaultCoinffeineNetwork(peer: ActorRef) extends CoinffeineNetwork {
  implicit private val timeout = Timeout(3.seconds)

  private var _status: CoinffeineNetwork.Status = Disconnected

  override def status = _status

  /** @inheritdoc
    *
    * With the centralized broker implementation over protobuf RPC, "connecting" consists on opening
    * a port with a duplex RPC server.
    */
  override def connect(): Future[Connected.type] = {
    _status = Connecting
    val bindResult = (peer ? PeerActor.Connect).flatMap {
      case PeerActor.Connected => Future.successful(Connected)
      case PeerActor.ConnectionFailed(cause) => Future.failed(ConnectException(cause))
    }
    bindResult.onComplete {
      case Success(connected) => _status = connected
      case Failure(_) => _status = Disconnected
    }
    bindResult
  }

  override def disconnect(): Future[Disconnected.type] = ???

  override def currentQuote[C <: FiatCurrency](currency: C): Future[Quote[C]] =
    (peer ? QuoteRequest(currency)).mapTo[Quote[C]]

  override def exchanges: Set[Exchange] = Set.empty

  override def onExchangeChanged(listener: ExchangeListener): Unit = ???

  override def orders: Set[Order] = Set.empty

  override def submitOrder(order: Order): Order = {
    peer ! OpenOrder(order)
    order
  }

  override def cancelOrder(order: Order): Unit = {
    peer ! CancelOrder(order)
  }
}
