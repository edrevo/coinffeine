package com.coinffeine.common.protocol

import java.math.BigInteger

import com.coinffeine.common.UnitTest
import com.coinffeine.common.bitcoin.TransactionSignature

class TransactionSignatureUtilsTest extends UnitTest {

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

  it should "have the same hash code when equal" in {
    val signature = new TransactionSignature(BigInteger.ZERO, BigInteger.ZERO)
    val sameSignature = new TransactionSignature(BigInteger.ZERO, BigInteger.ZERO)
    val otherSignature = new TransactionSignature(BigInteger.ONE, BigInteger.ONE)
    TransactionSignatureUtils.hashCode(signature) should (
      be (TransactionSignatureUtils.hashCode(sameSignature)) and
      not be TransactionSignatureUtils.hashCode(otherSignature))
  }
}
