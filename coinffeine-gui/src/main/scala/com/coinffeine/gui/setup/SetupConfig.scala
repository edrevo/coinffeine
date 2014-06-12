package com.coinffeine.gui.setup

import com.coinffeine.common.paymentprocessor.okpay.OkPayCredentials

/** Initial setup configuration.
  *
  * Note that all fields are optionals as the user is not forced to fill them in.
  *
  * @param password          Password to protect the application
  * @param okPayCredentials  OKPay credentials
  */
case class SetupConfig(password: Option[String], okPayCredentials: Option[OkPayCredentials])
