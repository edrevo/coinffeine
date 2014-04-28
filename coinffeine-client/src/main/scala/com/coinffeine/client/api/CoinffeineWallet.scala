package com.coinffeine.client.api

import java.security.interfaces.ECKey

import scala.concurrent.Future

import com.coinffeine.common.currency.BtcAmount
import com.google.bitcoin.core.{Address, Sha256Hash}

trait CoinffeineWallet {

  def currentBalance(): BtcAmount

  /** Where to transfer BTC funds to top-up Coinffeine */
  def depositAddress: Address

  def importPrivateKey(address: Address, key: ECKey): Unit

  /** Transfer a given amount of BTC to an address if possible.
    *
    * @param amount   Amount to transfer
    * @param address  Destination address
    * @return         TX id if transfer is possible, TransferException otherwise
    */
  def transfer(amount: BtcAmount, address: Address): Future[Sha256Hash]
}

object CoinffeineWallet {

  case class TransferException(amount: BtcAmount, address: Address, cause: Throwable)
    extends Exception(s"Cannot transfer $amount to $address", cause)
}
