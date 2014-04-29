package com.coinffeine.common.config

import com.typesafe.config.Config

/** Cake-pattern provider of configurations */
trait ConfigComponent {
  def config: Config
}
