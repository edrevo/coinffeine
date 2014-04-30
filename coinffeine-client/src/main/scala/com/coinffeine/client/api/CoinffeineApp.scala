package com.coinffeine.client.api

import java.io.Closeable

import com.coinffeine.common.paymentprocessor.PaymentProcessor

/** Coinffeine application interface */
trait CoinffeineApp extends Closeable {
  def network: CoinffeineNetwork
  def wallet: CoinffeineWallet
  def paymentProcessors: Set[PaymentProcessor]
  def marketStats: MarketStats
}

