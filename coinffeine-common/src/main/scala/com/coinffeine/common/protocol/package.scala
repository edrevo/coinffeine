package com.coinffeine.common

import com.coinffeine.common.currency.FiatAmount

package object protocol {
  type Spread = (Option[FiatAmount], Option[FiatAmount])
}
