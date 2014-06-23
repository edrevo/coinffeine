package com.coinffeine.common.exchange.impl

import com.coinffeine.common.BitcoinAmount
import com.google.bitcoin.core.{ECKey, NetworkParameters}

private[impl] case class UnsignedRefundTransaction(
    deposit: ImmutableTransaction,
    outputKey: ECKey,
    outputAmount: BitcoinAmount,
    lockTime: Long,
    network: NetworkParameters)
  extends ImmutableTransaction(
    TransactionProcessor.createUnsignedTransaction(
      inputs = Seq(deposit.get.getOutput(0)),
      outputs = Seq(outputKey -> outputAmount),
      network = network,
      lockTime = Some(lockTime)
    ))
