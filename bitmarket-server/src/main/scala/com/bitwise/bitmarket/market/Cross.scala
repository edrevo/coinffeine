package com.bitwise.bitmarket.market

import com.bitwise.bitmarket.common.currency.{FiatAmount, BtcAmount}

/** Represents a coincidence of desires of both a buyer and a seller */
case class Cross(
    amount: BtcAmount,
    price: FiatAmount,
    buyer: String,
    seller: String
)
