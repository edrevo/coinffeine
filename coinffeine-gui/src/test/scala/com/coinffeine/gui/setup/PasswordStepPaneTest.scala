package com.coinffeine.gui.setup

import javafx.scene.Node
import javafx.scene.control.Label

import com.coinffeine.gui.GuiTest

class PasswordStepPaneTest extends GuiTest[PasswordStepPane]  {

  override def createRootNode(): PasswordStepPane = new PasswordStepPane()

  "The password step pane" should "disable the password fields if no password is selected" in new Fixture {
    click("#noPassword")
    find[Node]("#repeatPasswordField") should be ('disabled)
    find[Node]("#passwordField") should be ('disabled)
    instance.canContinue.value should be (true)
  }

  it should "re-enable the password fields if the password option is re-selected" in new Fixture {
    click("#noPassword")
    find[Node]("#repeatPasswordField") should be ('disabled)
    find[Node]("#passwordField") should be ('disabled)
    click("#usePassword")
    find[Node]("#repeatPasswordField") should not be ('disabled)
    find[Node]("#passwordField") should not be ('disabled)
    instance.canContinue.value should be (false)

  }

  it should "warn the user about weak passwords" in new Fixture {
    find[Label]("#passwordWarningLabel").getText should be ('empty)
    click("#passwordField").`type`("weak")
    find[Label]("#passwordWarningLabel").getText should include ("weak")
    instance.canContinue.value should be (false)
  }

  it should "not warn the user about when password is strong" in new Fixture {
    find[Label]("#passwordWarningLabel").getText should be ('empty)
    click("#passwordField").`type`("ThisIsASuperStrongPassw0rd")
    find[Label]("#passwordWarningLabel").getText should be ('empty)
    instance.canContinue.value should be (false)
  }

  it should "warn the user about mismatching passwords" in new Fixture {
    find[Label]("#passwordWarningLabel").getText should be ('empty)
    click("#passwordField").`type`("CorrectPassword")
    click("#repeatPasswordField").`type`("TypoPassword")
    find[Label]("#passwordWarningLabel").getText should include ("don't match")
    instance.canContinue.value should be (false)
  }

  it should "let the user continue is passwords are OK" in new Fixture {
    find[Label]("#passwordWarningLabel").getText should be ('empty)
    click("#passwordField").`type`("CorrectPassword")
    click("#repeatPasswordField").`type`("CorrectPassword")
    find[Label]("#passwordWarningLabel").getText should be ('empty)
    instance.canContinue.value should be (true)
  }
}
