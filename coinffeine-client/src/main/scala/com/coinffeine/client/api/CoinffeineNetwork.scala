package com.coinffeine.client.api

import scala.concurrent.Future

import com.coinffeine.common._
import com.coinffeine.common.protocol.messages.brokerage.Quote

/** Represents how the app takes part on the P2P network */
trait CoinffeineNetwork {

  def status: CoinffeineNetwork.Status

  /** Start connection with the network.
    *
    * @return The connected status in case of success or ConnectException otherwise
    */
  def connect(): Future[CoinffeineNetwork.Connected.type]

  /** Disconnect from the network.
    *
    * @return A future to be resolved when actually disconnected from the network.
    */
  def disconnect(): Future[CoinffeineNetwork.Disconnected.type]

  def orders: Set[Order]
  def exchanges: Set[Exchange]

  /** Notify exchange events. */
  def onExchangeChanged(listener: CoinffeineNetwork.ExchangeListener): Unit

  /** Submit an order to buy bitcoins.
    *
    * @param btcAmount           Amount to buy
    * @param fiatAmount          Fiat money to use
    * @return                    A new exchange if submitted successfully
    */
  def submitBuyOrder(btcAmount: BitcoinAmount, fiatAmount: FiatAmount): Order =
    submitOrder(Order(Bid, btcAmount, fiatAmount))

  /** Submit an order to sell bitcoins.
    *
    * @param btcAmount           Amount to sell
    * @param fiatAmount          Fiat money to use
    * @return                    A new exchange if submitted successfully
    */
  def submitSellOrder(btcAmount: BitcoinAmount, fiatAmount: FiatAmount): Order =
    submitOrder(Order(Ask, btcAmount, fiatAmount))

  /** Submit an order. */
  def submitOrder(order: Order): Order

  def cancelBuyOrder(btcAmount: BitcoinAmount, fiatAmount: FiatAmount): Unit = {
    cancelOrder(Order(Bid, btcAmount, fiatAmount))
  }

  def cancelSellOrder(btcAmount: BitcoinAmount, fiatAmount: FiatAmount): Unit = {
    cancelOrder(Order(Ask, btcAmount, fiatAmount))
  }

  /** Cancel an unmatched order. */
  def cancelOrder(order: Order): Unit
}

object CoinffeineNetwork {

  sealed trait Status
  case object Disconnected extends Status
  case object Connected extends Status
  case object Connecting extends Status

  case class ConnectException(cause: Throwable)
    extends Exception("Cannot connect to the P2P network", cause)

  trait ExchangeListener {
    def onNewExchange(exchange: Exchange): Unit
    def onExchangeChange(exchange: Exchange): Unit
  }
}
