package com.coinffeine.common.currency

import com.coinffeine.common.UnitTest
import com.coinffeine.common.currency.Implicits._

class BtcAmountTest extends UnitTest {

  "BTC amounts" should "be converted to satoshis" in {
    1.BTC.asSatoshi.longValue() should be (BtcAmount.OneBtcInSatoshi.longValue())
  }

  it should "be converted to string" in {
    1.3.BTC.toString should be ("1.3 BTC")
  }

  it should "support arithmetic" in {
    (9.BTC - 5.BTC - (- 1.BTC)) * 2 should be (10.BTC)
    10.BTC / 10 should be (1.BTC)
    5.BTC should be > 1.BTC
  }
}
