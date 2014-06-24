package com.coinffeine.common.protocol.messages.exchange

import com.coinffeine.common.protocol.messages.PublicMessage

case class PaymentProof(exchangeId: ExchangeId, paymentId: String) extends PublicMessage
