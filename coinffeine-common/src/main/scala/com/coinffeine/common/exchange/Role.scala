package com.coinffeine.common.exchange

import com.coinffeine.common._

sealed trait Role {
  def me(exchange: Exchange[_ <: FiatCurrency]): Exchange.PeerInfo[exchange.KeyPair]
  def her(exchange: Exchange[_ <: FiatCurrency]): Exchange.PeerInfo[exchange.KeyPair]
  def myDepositAmount(amounts: Exchange.Amounts[_ <: FiatCurrency]): BitcoinAmount
  def myRefundAmount(amounts: Exchange.Amounts[_ <: FiatCurrency]): BitcoinAmount
}

object BuyerRole extends Role {

  override def toString = "buyer"

  override def me(exchange: Exchange[_ <: FiatCurrency]): Exchange.PeerInfo[exchange.KeyPair] =
    exchange.buyer

  override def her(exchange: Exchange[_ <: FiatCurrency]): Exchange.PeerInfo[exchange.KeyPair] =
    exchange.seller

  override def myDepositAmount(amounts: Exchange.Amounts[_ <: FiatCurrency]): BitcoinAmount =
    amounts.buyerDeposit

  override def myRefundAmount(amounts: Exchange.Amounts[_ <: FiatCurrency]): BitcoinAmount =
    amounts.buyerRefund
}

object SellerRole extends Role {

  override def toString = "seller"

  override def me(exchange: Exchange[_ <: FiatCurrency]): Exchange.PeerInfo[exchange.KeyPair] =
    exchange.seller

  override def her(exchange: Exchange[_ <: FiatCurrency]): Exchange.PeerInfo[exchange.KeyPair] =
    exchange.buyer

  override def myDepositAmount(amounts: Exchange.Amounts[_ <: FiatCurrency]): BitcoinAmount =
    amounts.sellerDeposit

  override def myRefundAmount(amounts: Exchange.Amounts[_ <: FiatCurrency]): BitcoinAmount =
    amounts.sellerRefund
}
