package com.coinffeine.acceptance.mockpay

import java.io.Closeable

/** Payment processor mock that can be used to create accounts, set balances, delay or make
  * transactions fail at will.
  */
class MockPay extends Closeable {
  override def close() {}
}
