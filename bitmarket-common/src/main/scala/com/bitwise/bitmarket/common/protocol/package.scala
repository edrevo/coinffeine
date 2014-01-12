package com.bitwise.bitmarket.common

import com.bitwise.bitmarket.common.currency.FiatAmount

package object protocol {
  type Spread = (Option[FiatAmount], Option[FiatAmount])
}
