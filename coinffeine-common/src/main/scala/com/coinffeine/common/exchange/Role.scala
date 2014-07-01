package com.coinffeine.common.exchange

import com.coinffeine.common._

sealed trait Role {
  def counterpart: Role
  def myDepositAmount(amounts: Exchange.Amounts[_ <: FiatCurrency]): BitcoinAmount
  def myRefundAmount(amounts: Exchange.Amounts[_ <: FiatCurrency]): BitcoinAmount
  def herRefundAmount(amounts: Exchange.Amounts[_ <: FiatCurrency]): BitcoinAmount
  def buyer[A](mine: A, her: A): A
  def seller[A](mine: A, her: A): A
}

object BuyerRole extends Role {

  override def counterpart = SellerRole

  override def toString = "buyer"

  override def myDepositAmount(amounts: Exchange.Amounts[_ <: FiatCurrency]): BitcoinAmount =
    amounts.buyerDeposit

  override def myRefundAmount(amounts: Exchange.Amounts[_ <: FiatCurrency]): BitcoinAmount =
    amounts.buyerRefund

  override def herRefundAmount(amounts: Exchange.Amounts[_ <: FiatCurrency]): BitcoinAmount =
    amounts.sellerRefund

  override def buyer[A](mine: A, her: A): A = mine

  override def seller[A](mine: A, her: A): A = her
}

object SellerRole extends Role {

  override def counterpart = BuyerRole

  override def toString = "seller"

  override def myDepositAmount(amounts: Exchange.Amounts[_ <: FiatCurrency]): BitcoinAmount =
    amounts.sellerDeposit

  override def myRefundAmount(amounts: Exchange.Amounts[_ <: FiatCurrency]): BitcoinAmount =
    amounts.sellerRefund

  override def herRefundAmount(amounts: Exchange.Amounts[_ <: FiatCurrency]): BitcoinAmount =
    amounts.buyerRefund

  override def buyer[A](mine: A, her: A): A = her

  override def seller[A](mine: A, her: A): A = mine
}
