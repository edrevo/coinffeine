package com.coinffeine.client

import scala.collection.JavaConversions._

import com.google.bitcoin.script.Script
import com.google.bitcoin.core.ECKey

case class MultiSigInfo(possibleKeys: Set[ECKey], requiredKeyCount: Int)

object MultiSigInfo {
  def apply(script: Script): MultiSigInfo = {
    require(script.isSentToMultiSig)
    val scriptChunks = script.getChunks
    val numberOfRequiredSignatures = Script.decodeFromOpN(
      scriptChunks(0).data(0))
    val numberOfKeys = Script.decodeFromOpN(scriptChunks(scriptChunks.size - 2).data(0))
    val possibleKeys = scriptChunks
      .take(scriptChunks.size - 2) // Last two chunks are number of keys and OP_CHECKMULTISIG
      .drop(1) // First chunk is number of required keys
      .map(chunk => new ECKey(null, chunk.data))
      .toSet
    require(possibleKeys.size == numberOfKeys)
    new MultiSigInfo(possibleKeys, numberOfRequiredSignatures)
  }
}
