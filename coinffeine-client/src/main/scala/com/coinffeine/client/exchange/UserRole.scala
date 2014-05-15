package com.coinffeine.client.exchange

import com.google.bitcoin.core.ECKey

/** A trait that provides role-specific access to fiat and btc information for both user
  * and counterpart
  */
sealed trait UserRole {
  /** The BTC key for the buyer */
  val buyersKey: ECKey

  /** The BTC key for the seller */
  val sellersKey: ECKey

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
trait BuyerUser extends UserRole {
  this: Exchange =>

  override lazy val buyersKey: ECKey = exchangeInfo.userKey
  override lazy val sellersKey: ECKey = exchangeInfo.counterpartKey
  override lazy val sellersFiatAddress: String = exchangeInfo.counterpartFiatAddress
  override lazy val buyersFiatAddress: String = exchangeInfo.userFiatAddress
  override lazy val userInputIndex: Int = 1
  override lazy val counterPartInputIndex: Int = 0
}

/** This trait can be mixed with an Exchange if the user is acting like a seller */
trait SellerUser extends UserRole {
  this: Exchange =>

  override lazy val buyersKey: ECKey = exchangeInfo.counterpartKey
  override lazy val sellersKey: ECKey = exchangeInfo.userKey
  override lazy val sellersFiatAddress: String = exchangeInfo.userFiatAddress
  override lazy val buyersFiatAddress: String = exchangeInfo.counterpartFiatAddress
  override lazy val userInputIndex: Int = 0
  override lazy val counterPartInputIndex: Int = 1
}
