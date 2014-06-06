package com.coinffeine.common.protocol.messages.brokerage

import java.util.Currency

import com.coinffeine.common.currency.{BtcAmount, FiatAmount}
import com.coinffeine.common.currency.Implicits._

case class VolumeByPrice(currency: Currency, entries: Map[FiatAmount, BtcAmount]) {

  require(entries.keys.forall(_.currency == currency), "Mixed currencies")
  requirePositiveValues()

  def highestPrice: Option[FiatAmount] = entries.keys.reduceOption(_ max _)
  def lowestPrice: Option[FiatAmount] = entries.keys.reduceOption(_ min _)

  def isEmpty = entries.isEmpty

  /** Volume at a given price */
  def volumeAt(price: FiatAmount): BtcAmount = entries.getOrElse(price, 0.BTC)

  def increase(price: FiatAmount, amount: BtcAmount): VolumeByPrice =
    copy(entries = entries.updated(price, volumeAt(price) + amount))

  def decrease(price: FiatAmount, amount: BtcAmount): VolumeByPrice = {
    val previousAmount = volumeAt(price)
    if (previousAmount > amount) copy(entries = entries.updated(price, previousAmount - amount))
    else copy(entries = entries - price)
  }

  private def requirePositiveValues(): Unit = {
    entries.foreach { case (price, amount) =>
        require(amount.amount > 0, "Amount ordered must be strictly positive")
        require(price.amount > 0, "Price must be strictly positive")
    }
  }

}

object VolumeByPrice {

  /** Convenience factory method */
  def apply(pair: (FiatAmount, BtcAmount), otherPairs: (FiatAmount, BtcAmount)*): VolumeByPrice = {
    val pairs = pair +: otherPairs
    val prices = pairs.map(_._1)
    require(prices.toSet.size == pairs.size, s"Repeated prices: ${prices.mkString(",")}")
    VolumeByPrice(pair._1.currency, pairs.toMap)
  }

  def empty(currency: Currency): VolumeByPrice =
    VolumeByPrice(currency, Map.empty[FiatAmount, BtcAmount])
}
