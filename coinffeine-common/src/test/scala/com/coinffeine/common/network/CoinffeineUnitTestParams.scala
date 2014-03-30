package com.coinffeine.common.network

import com.google.bitcoin.params.UnitTestParams

object CoinffeineUnitTestParams extends UnitTestParams {
  // Ensures difficulty stays at minimum level
  interval = Int.MaxValue

  // Ensures that bitcoins are spendable as soon as they are mined
  spendableCoinbaseDepth = 0

  // Ensures that the miner's reward for each block is constant
  subsidyDecreaseBlockCount = Int.MaxValue

  trait Component extends NetworkComponent {
    override val network = CoinffeineUnitTestParams
  }
}
