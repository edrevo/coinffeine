package com.coinffeine.common.protocol

import scala.util.Random

import org.mockito.BDDMockito.given
import org.scalatest.mock.MockitoSugar

import com.coinffeine.common.bitcoin.{Hash, MutableTransaction}

/** Factory of mocked transactions */
object MockTransaction extends MockitoSugar {

  /** Creates a mocked transaction with random hash and random serialization. */
  def apply(): MutableTransaction = {
    val tx = mock[MutableTransaction]
    val hash = randomHash()
    val serializedTransaction = randomByteArray(16)
    given(tx.getHash).willReturn(hash)
    given(tx.bitcoinSerialize).willReturn(serializedTransaction)
    tx
  }

  private def randomHash() = new Hash(randomByteArray(32))

  private def randomByteArray(len: Int) = Array.fill(len)(Random.nextInt(256).toByte)
}
