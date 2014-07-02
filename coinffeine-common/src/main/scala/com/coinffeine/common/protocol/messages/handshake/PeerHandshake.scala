package com.coinffeine.common.protocol.messages.handshake

import com.coinffeine.common.bitcoin.ImmutableTransaction
import com.coinffeine.common.exchange.Exchange
import com.coinffeine.common.paymentprocessor.PaymentProcessor
import com.coinffeine.common.protocol.messages.PublicMessage

case class PeerHandshake(
    exchangeId: Exchange.Id,
    refundTx: ImmutableTransaction,
    paymentProcessorAccount: PaymentProcessor.AccountId) extends PublicMessage
