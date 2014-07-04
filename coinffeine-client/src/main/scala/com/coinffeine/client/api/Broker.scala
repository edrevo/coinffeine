package com.coinffeine.client.api

import scala.concurrent.Future

import com.coinffeine.common.Order

trait Broker {

  def submitOrder(order: Order): Future[Unit]
}
