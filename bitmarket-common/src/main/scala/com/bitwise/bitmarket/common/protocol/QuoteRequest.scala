package com.bitwise.bitmarket.common.protocol

import java.util.Currency

/** Used to ask about the current quote of bitcoin traded in a given currency */
case class QuoteRequest(currency: Currency)
