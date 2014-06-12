package com.coinffeine.gui.setup

import scala.util.{Failure, Success}
import scala.util.control.NonFatal

import org.slf4j.LoggerFactory
import scalafx.Includes._
import scalafx.beans.property.{BooleanProperty, ObjectProperty, StringProperty}
import scalafx.event.ActionEvent
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control._
import scalafx.scene.layout._
import scalafx.scene.text.Font

import com.coinffeine.common.paymentprocessor.okpay.OkPayCredentials
import com.coinffeine.gui.concurrent.ScalaFxImplicits._
import com.coinffeine.gui.wizard.StepPane

private[setup] class OkPayCredentialsStepPane(credentialsValidator: CredentialsValidator)
  extends StackPane with StepPane[SetupConfig] {

  private val emailProperty = new StringProperty(this, "email", "")
  emailProperty.onChange { updateCredentials() }
  private val passwordProperty = new StringProperty(this, "password", "")
  passwordProperty.onChange { updateCredentials() }
  private val credentials = new ObjectProperty[Option[OkPayCredentials]](this, "credentials", None)
  private val testMessage = new StringProperty(this, "testMessage", "")
  private val testing = new BooleanProperty(this, "testing", false)

  private val testButton = new Button("Test") {
    disable <== testing.or(credentials === None)
    handleEvent(ActionEvent.ACTION) { () => startTest() }
  }

  content = {
    val credentialsTestPane = new HBox(spacing = 5) {
      hgrow = Priority.ALWAYS
      alignment = Pos.CENTER_LEFT
      content = Seq(
        testButton,
        new Label { text <== testMessage },
        new ProgressIndicator {
          progress <== when(testing) choose ProgressIndicator.INDETERMINATE_PROGRESS otherwise 0
          visible <== testing
          prefWidth = 18
          prefHeight = 18
        }
      )
    }

    val grid = new GridPane {
      padding = Insets(20, 10, 0, 10)
      hgap = 5
      vgap = 5
      columnConstraints = Seq(new ColumnConstraints {
        prefWidth = 100
        fillWidth = false
        hgrow = Priority.NEVER
      }, new ColumnConstraints {
        fillWidth = true
        hgrow = Priority.ALWAYS
      })
      add(new Label("Email"), 0, 0)
      add(new TextField() { text <==> emailProperty }, 1, 0)
      add(new Label("Password"), 0, 1)
      add(new PasswordField() { text <==> passwordProperty }, 1, 1)
      add(credentialsTestPane, 1, 2)
    }

    new VBox(spacing = 5) {
      padding = Insets(10, 50, 10, 50)
      content = Seq(
        new Label("Configure your OKPay account") { font = Font(16) },
        new Label("Your credentials are stored locally and never will be shared"),
        grid
      )
    }
  }

  override def bindTo(data: ObjectProperty[SetupConfig]): Unit = {
    canContinue.value = true
    credentials.onChange {
      data.value = data.value.copy(okPayCredentials = credentials.value)
    }
  }

  private def startTest(): Unit = {
    testing.value = true
    testMessage.value = ""
    val validation =
      credentialsValidator(OkPayCredentials(emailProperty.value, passwordProperty.value))
    validation.onComplete {
      case Success(CredentialsValidator.ValidCredentials) =>
        testMessage.value = "OK"
      case Success(CredentialsValidator.InvalidCredentials(message)) =>
        testMessage.value = message
      case Failure(NonFatal(cause)) =>
        OkPayCredentialsStepPane.Log.error("Unexpected error when testing credentials", cause)
        testMessage.value = "unexpected error"
    }
    validation.onComplete { case _ => testing.value = false }
  }

  private def updateCredentials(): Unit = {
    credentials.value = (emailProperty.value, passwordProperty.value) match {
      case (email, password) if !email.isEmpty && !password.isEmpty =>
        Some(OkPayCredentials(email, password))
      case _ => None
    }
  }
}

private[setup] object OkPayCredentialsStepPane {
  val Log = LoggerFactory.getLogger(classOf[OkPayCredentialsStepPane])
}
