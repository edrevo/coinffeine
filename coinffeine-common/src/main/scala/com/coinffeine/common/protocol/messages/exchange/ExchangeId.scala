package com.coinffeine.common.protocol.messages.exchange

import java.security.SecureRandom
import scala.util.Random

case class ExchangeId(value: String) {
  override def toString = s"exchange:$value"
}

object ExchangeId {
  private val secureGenerator = new Random(new SecureRandom())

  def random() = new ExchangeId(value = secureGenerator.nextString(12))
}
