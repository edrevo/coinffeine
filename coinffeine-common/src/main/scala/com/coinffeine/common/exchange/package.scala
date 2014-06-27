package com.coinffeine.common

import com.coinffeine.common.bitcoin.ImmutableTransaction

package object exchange {
  type Deposits = Both[ImmutableTransaction]
  val Deposits = Both
}
