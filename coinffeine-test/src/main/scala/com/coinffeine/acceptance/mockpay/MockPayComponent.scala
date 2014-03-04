package com.coinffeine.acceptance.mockpay

trait MockPayComponent {
  lazy val mockPay: MockPay = new MockPay
}
