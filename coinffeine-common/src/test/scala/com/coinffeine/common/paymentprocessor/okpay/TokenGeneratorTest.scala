package com.coinffeine.common.paymentprocessor.okpay

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.joda.time.format.DateTimeFormat

class TokenGeneratorTest extends FlatSpec with ShouldMatchers with MockitoSugar {

  val instance = new TokenGenerator("seedToken")

  "TokenGenerator" must "generate a valid token using a date" in {
    val formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss")
    val token = instance.build(formatter.parseDateTime("20/02/2014 14:00:00"))
    token should be ("CF49014918670822614A5816F10B8C1047FBD7FF2A653848F60BBE52821C0F72")
  }
}
