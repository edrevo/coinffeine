package com.coinffeine.client.api

import com.coinffeine.common.paymentprocessor.PaymentProcessor

/** Coinffeine application interface */
trait CoinffeineApp {
  def network: CoinffeineNetwork
  def wallet: CoinffeineWallet
  def paymentProcessors: Set[PaymentProcessor]
  def marketStats: MarketStats
}

