package com.bitwise.bitmarket

import com.bitwise.bitmarket.common.currency.FiatAmount

package object market {
  type Spread = (Option[FiatAmount], Option[FiatAmount])
}
