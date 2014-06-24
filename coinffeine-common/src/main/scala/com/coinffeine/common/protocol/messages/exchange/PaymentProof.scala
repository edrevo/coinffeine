package com.coinffeine.common.protocol.messages.exchange

import com.coinffeine.common.exchange.Exchange
import com.coinffeine.common.protocol.messages.PublicMessage

case class PaymentProof(exchangeId: Exchange.Id, paymentId: String) extends PublicMessage
