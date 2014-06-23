package com.coinffeine.common.exchange.impl

import com.coinffeine.common.BitcoinAmount
import com.coinffeine.common.bitcoin.{ImmutableTransaction, Network, PublicKey}

private[impl] case class UnsignedRefundTransaction(
    deposit: ImmutableTransaction,
    outputKey: PublicKey,
    outputAmount: BitcoinAmount,
    lockTime: Long,
    network: Network)
  extends ImmutableTransaction(
    TransactionProcessor.createUnsignedTransaction(
      inputs = Seq(deposit.get.getOutput(0)),
      outputs = Seq(outputKey -> outputAmount),
      network = network,
      lockTime = Some(lockTime)
    ))
