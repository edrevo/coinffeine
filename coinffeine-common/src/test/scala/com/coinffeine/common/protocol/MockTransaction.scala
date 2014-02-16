package com.coinffeine.common.protocol

import com.google.bitcoin.core.{Sha256Hash, Transaction}
import org.mockito.BDDMockito.given
import org.scalatest.mock.MockitoSugar
import scala.util.Random

/** Factory of mocked transactions */
object MockTransaction extends MockitoSugar {

  /** Creates a mocked transaction with random hash and random serialization. */
  def apply(): Transaction = {
    val tx = mock[Transaction]
    val hash = randomHash()
    val serializedTransaction = randomByteArray(16)
    given(tx.getHash).willReturn(hash)
    given(tx.bitcoinSerialize).willReturn(serializedTransaction)
    tx
  }

  private def randomHash() = new Sha256Hash(randomByteArray(32))

  private def randomByteArray(len: Int) = Array.fill(len)(Random.nextInt(256).toByte)
}
