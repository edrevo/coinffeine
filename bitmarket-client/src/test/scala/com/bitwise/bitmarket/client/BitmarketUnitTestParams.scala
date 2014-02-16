package com.bitwise.bitmarket.client

import com.google.bitcoin.params.UnitTestParams

object BitmarketUnitTestParams extends UnitTestParams {
  // Ensures difficulty stays at minimum level
  interval = Int.MaxValue

  // Ensures that bitcoins are spendable as soon as they are mined
  spendableCoinbaseDepth = 0

  // Ensures that the miner's reward for each block is constant
  subsidyDecreaseBlockCount = Int.MaxValue
}
