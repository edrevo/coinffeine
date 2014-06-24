package com.coinffeine.client.exchange

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.KeyPair

/** A trait that provides role-specific access to fiat and btc information for both user
  * and counterpart
  */
sealed trait UserRole {
  /** The BTC key for the buyer */
  val buyersKey: KeyPair

  /** The BTC key for the seller */
  val sellersKey: KeyPair

  /** The fiat address for the seller */
  val sellersFiatAddress: String

  /** The fiat address for the buyer */
  val buyersFiatAddress: String

  /** The input index that contains the user's funds in an exchange transaction (see TX i in
    * https://github.com/Coinffeine/coinffeine/wiki/Exchange-algorithm#transaction-definitions)
    */
  val userInputIndex: Int

  /** The input index that contains the counterpart's funds in an exchange transaction (see TX i in
    * https://github.com/Coinffeine/coinffeine/wiki/Exchange-algorithm#transaction-definitions)
    */
  val counterPartInputIndex: Int
}

/** This trait can be mixed with an Exchange if the user is acting like a buyer */
trait BuyerUser[C <: FiatCurrency] extends UserRole {
  this: Exchange[C] =>

  override lazy val buyersKey: KeyPair = exchangeInfo.user.bitcoinKey
  override lazy val sellersKey: KeyPair = exchangeInfo.counterpart.bitcoinKey
  override lazy val sellersFiatAddress: String = exchangeInfo.counterpart.paymentProcessorAccount
  override lazy val buyersFiatAddress: String = exchangeInfo.user.paymentProcessorAccount
  override lazy val userInputIndex: Int = 1
  override lazy val counterPartInputIndex: Int = 0
}

/** This trait can be mixed with an Exchange if the user is acting like a seller */
trait SellerUser[C <: FiatCurrency] extends UserRole {
  this: Exchange[C] =>

  override lazy val buyersKey: KeyPair = exchangeInfo.counterpart.bitcoinKey
  override lazy val sellersKey: KeyPair = exchangeInfo.user.bitcoinKey
  override lazy val sellersFiatAddress: String = exchangeInfo.user.paymentProcessorAccount
  override lazy val buyersFiatAddress: String = exchangeInfo.counterpart.paymentProcessorAccount
  override lazy val userInputIndex: Int = 0
  override lazy val counterPartInputIndex: Int = 1
}
