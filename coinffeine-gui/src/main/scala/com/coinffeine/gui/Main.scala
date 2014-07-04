package com.coinffeine.gui

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Random

import com.typesafe.config.ConfigFactory
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage

import com.coinffeine.client.app.ProductionCoinffeineApp
import com.coinffeine.common.Order
import com.coinffeine.common.paymentprocessor.okpay.OkPayCredentials
import com.coinffeine.gui.application.ApplicationScene
import com.coinffeine.gui.application.main.MainView
import com.coinffeine.gui.application.operations.OperationsView
import com.coinffeine.gui.setup.{CredentialsValidator, SetupWizard}
import com.coinffeine.gui.setup.CredentialsValidator.Result
import com.coinffeine.gui.util.ProgressDialog

object Main extends JFXApp with ProductionCoinffeineApp.Component {

  override val config = ConfigFactory.load()

  JFXApp.AUTO_SHOW = false

  val validator = new CredentialsValidator {
    override def apply(credentials: OkPayCredentials): Future[Result] = Future {
      Thread.sleep(2000)
      if (Random.nextBoolean()) CredentialsValidator.ValidCredentials
      else CredentialsValidator.InvalidCredentials("Random failure")
    }
  }
  val sampleAddress = "124U4qQA7g33C4YDJFpwqXd2XJiA3N6Eb7"
  val setupConfig = new SetupWizard(sampleAddress, validator).run()

  stage = new PrimaryStage {
    title = "Coinffeine"
    scene = new ApplicationScene(views = Seq(new MainView, new OperationsView(onOrderSubmitted)))
  }
  stage.show()

  private def onOrderSubmitted(order: Order): Unit = {
    val progress =
      ProgressDialog("Submitting order", "Submitting order to the broker...") {
        val submission = app.broker.submitOrder(order)
        Await.ready(submission, 10.seconds) // TODO: obtain this timeout from config
      }
    progress.show()
  }
}
