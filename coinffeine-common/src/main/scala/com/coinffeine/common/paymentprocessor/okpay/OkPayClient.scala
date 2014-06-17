package com.coinffeine.common.paymentprocessor.okpay

import com.coinffeine.common.paymentprocessor.okpay.generated._

trait OKPayClient {

  def service: I_OkPayAPI
}

object OKPayClient {

  trait Component {

    def okPayClient: OKPayClient
  }
}
