package com.coinffeine.gui.setup

import com.coinffeine.common.paymentprocessor.okpay.OkPayCredentials

import scala.concurrent.Future

/** Test OKPay credentials by contacting the service */
trait CredentialsTester {
  def apply(credentials: OkPayCredentials): Future[CredentialsTester.Result]
}

object CredentialsTester {
  sealed trait Result
  case object ValidCredentials extends Result
  case class InvalidCredentials(cause: String) extends Result
}

