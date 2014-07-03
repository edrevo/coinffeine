package com.coinffeine.client.app

import scala.concurrent._

import akka.actor.ActorRef

import com.coinffeine.client.api.Broker
import com.coinffeine.client.peer.PeerActor.OpenOrder
import com.coinffeine.common.Order

class DefaultBroker(peer: ActorRef) extends Broker {

  override def submitOrder(order: Order): Future[Unit] = {
    peer.tell(OpenOrder(order), ActorRef.noSender)
    Future.successful()
  }
}
