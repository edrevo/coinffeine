package com.coinffeine.common.paymentprocessor.okpay

import java.security.MessageDigest

import org.joda.time.DateTime

/** Generates crypto tokens based on current datetime */
private[okpay] class DefaultTokenGenerator(seedToken: String) extends TokenGenerator {

  override def build(currentTime: DateTime): String = {
    val date = currentTime.toString("yyyyMMdd")
    val hour = currentTime.toString("HH")
    val currentToken = String.format("%s:%s:%s", seedToken, date, hour)
    val hash = MessageDigest.getInstance("SHA-256").digest(currentToken.getBytes("UTF-8"))
    hash.map("%02X" format _).mkString
  }
}

object DefaultTokenGenerator {

  trait Component extends TokenGenerator.Component {
    override def createTokenGenerator(seedToken: String): TokenGenerator =
      new DefaultTokenGenerator(seedToken)
  }
}
