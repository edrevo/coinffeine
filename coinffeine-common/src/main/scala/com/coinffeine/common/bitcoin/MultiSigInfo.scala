package com.coinffeine.common.bitcoin

import com.google.bitcoin.script.{ScriptChunk, Script}

import scala.collection.JavaConversions._

case class MultiSigInfo(possibleKeys: Seq[PublicKey], requiredKeyCount: Int)

object MultiSigInfo {

  /** Tries to extract multisig information from an script */
  def fromScript(script: Script): Option[MultiSigInfo] = for (chunks <- requireMultisigChunks(script))
    yield MultiSigInfo(decodeKeys(chunks), decodeRequiredKeyCount(chunks))

  private def requireMultisigChunks(script: Script): Option[Seq[ScriptChunk]] =
    if (script.isSentToMultiSig) Some(script.getChunks) else None

  private def decodeRequiredKeyCount(chunks: Seq[ScriptChunk]): Int =
    Script.decodeFromOpN(chunks(0).data(0))

  private def decodeKeys(chunks: Seq[ScriptChunk]): Seq[PublicKey] = chunks
    .take(chunks.size - 2) // Last two chunks are number of keys and OP_CHECKMULTISIG
    .drop(1) // First chunk is number of required keys
    .map(chunk => new PublicKey(null, chunk.data))
}
