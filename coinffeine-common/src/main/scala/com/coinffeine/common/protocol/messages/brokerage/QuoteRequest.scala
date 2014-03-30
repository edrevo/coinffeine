package com.coinffeine.common.protocol.messages.brokerage

import java.util.Currency

import com.coinffeine.common.protocol.messages.PublicMessage

/** Used to ask about the current quote of bitcoin traded in a given currency */
case class QuoteRequest(currency: Currency) extends PublicMessage
