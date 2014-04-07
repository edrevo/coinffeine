package com.coinffeine.common.protocol

import java.math.BigInteger

import com.google.bitcoin.crypto.TransactionSignature
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

class TransactionSignatureUtilsTest extends FlatSpec with ShouldMatchers {

  "Transaction signatures" should "be equal when comparing null values" in {
    TransactionSignatureUtils.equals(null, null) should be (true)
  }

  it should "not be equal when comparing null with non-null values" in {
    val signature = new TransactionSignature(BigInteger.ZERO, BigInteger.ZERO)
    TransactionSignatureUtils.equals(null, signature) should be (false)
    TransactionSignatureUtils.equals(signature, null) should be (false)
  }

  it should "compare their encodings for pairs of non-null values" in {
    val s1 = new TransactionSignature(BigInteger.ZERO, BigInteger.ONE)
    val s2 = new TransactionSignature(BigInteger.ONE, BigInteger.ZERO)
    TransactionSignatureUtils.equals(s1, s1) should be (true)
    TransactionSignatureUtils.equals(s1, s2) should be (false)
  }
}
