package com.coinffeine.common.paymentprocessor.okpay

import java.security.MessageDigest

import org.joda.time.DateTime

/** Generates crypto tokens based on current datetime */
private[okpay] trait TokenGenerator {

  def build(currentTime: DateTime): String
}

object TokenGenerator {

  trait Component {
    def createTokenGenerator(seedToken: String): TokenGenerator
  }
}
