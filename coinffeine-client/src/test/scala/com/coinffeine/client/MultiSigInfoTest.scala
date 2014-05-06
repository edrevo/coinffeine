package com.coinffeine.client

import scala.collection.JavaConversions._

import com.google.bitcoin.core.ECKey
import com.google.bitcoin.crypto.TransactionSignature
import com.google.bitcoin.script.ScriptBuilder

import com.coinffeine.common.UnitTest

class MultiSigInfoTest extends UnitTest {

  "MultiSigInfo" should "read all the parameters of a multisig output script" in {
    val keys = List.fill(10)(new ECKey())
    val requiredKeys = 7
    val script = ScriptBuilder.createMultiSigOutputScript(requiredKeys, keys)
    val multiSigInfo = MultiSigInfo(script)
    multiSigInfo.possibleKeys.size should be (keys.size)
    multiSigInfo.requiredKeyCount should be (requiredKeys)
    multiSigInfo.possibleKeys should be (keys.toSet)
  }

  it should "fail on non-multisig scripts" in {
    val script = ScriptBuilder.createOutputScript(new ECKey())
    an [IllegalArgumentException] should be thrownBy MultiSigInfo(script)
  }

  it should "fail on input multisig scripts" in {
    val script = ScriptBuilder.createMultiSigInputScript(List(
      new TransactionSignature(BigInt(0).underlying(), BigInt(0).underlying())))
    an [IllegalArgumentException] should be thrownBy MultiSigInfo(script)
  }
}
