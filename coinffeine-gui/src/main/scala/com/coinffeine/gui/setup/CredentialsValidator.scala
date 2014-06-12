package com.coinffeine.gui.setup

import com.coinffeine.common.paymentprocessor.okpay.OkPayCredentials

import scala.concurrent.Future

/** Test OKPay credentials by contacting the service */
trait CredentialsValidator {
  def apply(credentials: OkPayCredentials): Future[CredentialsValidator.Result]
}

object CredentialsValidator {
  sealed trait Result
  case object ValidCredentials extends Result
  case class InvalidCredentials(cause: String) extends Result
}

