package com.coinffeine.gui

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage

import com.coinffeine.common.paymentprocessor.okpay.OkPayCredentials
import com.coinffeine.gui.setup.{CredentialsTester, SetupWizard}
import com.coinffeine.gui.setup.CredentialsTester.Result

object Main extends JFXApp {
  JFXApp.AUTO_SHOW = false

  val credentialsTest = new CredentialsTester {
    override def apply(credentials: OkPayCredentials): Future[Result] = Future {
      Thread.sleep(2000)
      if (Random.nextBoolean()) CredentialsTester.ValidCredentials
      else CredentialsTester.InvalidCredentials("Random failure")
    }
  }
  val setupConfig = new SetupWizard(credentialsTest).show()
  println(setupConfig)

  stage = new PrimaryStage {
    title = "Coinffeine"
    scene = new MainScene
  }
  stage.show()
}
