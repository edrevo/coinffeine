package com.coinffeine.common.blockchain

import scala.util.control.NonFatal

import com.google.bitcoin.core.{Transaction, TransactionBroadcaster}
import com.google.common.util.concurrent.{ListenableFuture, SettableFuture}

class MockTransactionBroadcaster extends TransactionBroadcaster {

  private var onBroadcast: Transaction => Transaction = recallTransaction
  private var _lastBroadcast: Option[Transaction] = None

  override def broadcastTransaction(tx: Transaction): ListenableFuture[Transaction] = {
    val result = SettableFuture.create[Transaction]()
    try {
      result.set(onBroadcast(tx))
    } catch {
      case NonFatal(e) => result.setException(e)
    }
    result
  }

  def lastBroadcast: Option[Transaction] = _lastBroadcast

  def givenSuccessOnTransactionBroadcast(): Unit = {
    onBroadcast = recallTransaction
  }

  def givenErrorOnTransactionBroadcast(error: Throwable): Unit = {
    onBroadcast = throwError(error)
  }

  private def recallTransaction(tx: Transaction): Transaction = {
    _lastBroadcast = Some(tx)
    tx
  }

  private def throwError(error: Throwable)(tx: Transaction): Transaction = {
    throw error
  }
}
