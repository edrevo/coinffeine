package com.coinffeine.gui.setup

import javafx.scene.Node
import javafx.scene.control.Label

import com.coinffeine.common.paymentprocessor.okpay.OkPayCredentials
import com.coinffeine.gui.GuiTest
import com.coinffeine.gui.setup.CredentialsValidator.{InvalidCredentials, ValidCredentials, Result}

import scala.concurrent.Future

class OkPayCredentialsStepPaneTest extends GuiTest[OkPayCredentialsStepPane] {
  private var validationResult: Future[Result] = _
  private val credentialsValidator = new CredentialsValidator {
    override def apply(credentials: OkPayCredentials): Future[Result] = validationResult
  }
  override def createRootNode(): OkPayCredentialsStepPane =
    new OkPayCredentialsStepPane(credentialsValidator)

  "The OKPay credentials step pane" should "have the test button disabled if there is no " +
    "email or password" in new Fixture {
      find[Node]("#testButton") should be ('disabled)
      val email = "email"
      click("#email").`type`(email)
      find[Node]("#testButton") should be ('disabled)
      click("#email").eraseCharacters(email.length)
      val password = "password"
      click("#password").`type`(password)
      find[Node]("#testButton") should be ('disabled)
      click("#email").`type`(email)
      click("#password").eraseCharacters(password.length)
      find[Node]("#testButton") should be ('disabled)
    }

  it should behave like testValidationResult(
    Future.successful(ValidCredentials), "valid credentials", "OK")

  locally {
    val myError = "this is my test error"
    it should behave like testValidationResult(
      Future.successful(InvalidCredentials(myError)), "invalid credentials", myError)
  }

  locally {
    val myError = "this is my unexpected error"
    it should behave like testValidationResult(
      Future.failed(new Error(myError)), "an unexpected error", myError)
  }

  def testValidationResult(result: Future[Result], validatorAction: String, expectedMessage: String) {
    it should s"report the expected message when the validator reports $validatorAction" in new Fixture {
      click("#email").`type`("email")
      click("#password").`type`("password")
      validationResult = Future.successful(InvalidCredentials(expectedMessage))
      click("#testButton")
      find[Label]("#validationResult").getText should include (expectedMessage)
    }
  }
}
