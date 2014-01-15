package com.bitwise.bitmarket.common.protocol

import com.bitwise.bitmarket.common.currency.{FiatAmount, BtcAmount}

/** Represents a coincidence of desires of both a buyer and a seller */
case class OrderMatch(
    amount: BtcAmount,
    price: FiatAmount,
    buyer: String,
    seller: String
)
