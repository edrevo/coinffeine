package com.coinffeine.common.protocol.messages.exchange

import com.coinffeine.common.protocol.messages.PublicMessage

case class PaymentProof(exchangeId: String, paymentId: String) extends PublicMessage
