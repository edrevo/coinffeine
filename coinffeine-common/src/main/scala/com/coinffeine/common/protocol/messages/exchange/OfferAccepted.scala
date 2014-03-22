package com.coinffeine.common.protocol.messages.exchange

import com.google.bitcoin.crypto.TransactionSignature

import com.coinffeine.common.protocol.messages.PublicMessage

case class OfferAccepted(exchangeId: String, signature: TransactionSignature) extends PublicMessage
