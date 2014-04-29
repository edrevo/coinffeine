package com.coinffeine.client.app

import com.coinffeine.client.api.CoinffeineApp

/** Cake-pattern provider of CoinffeineApp */
trait CoinffeineAppComponent {
  def app: CoinffeineApp
}
