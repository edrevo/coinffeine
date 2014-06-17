package com.coinffeine.gui.setup

import scalafx.beans.property.ObjectProperty
import scalafx.event.Event
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Label, PasswordField, RadioButton, ToggleGroup}
import scalafx.scene.layout._
import scalafx.scene.paint.Color
import scalafx.scene.text.TextAlignment

import com.coinffeine.gui.wizard.StepPane

private[setup] class PasswordStepPane extends StackPane with StepPane[SetupConfig] {

  private val passwordValidator = new PasswordValidator

  /** Mutable password to be updated when controls produce events */
  private val password = new ObjectProperty[Option[String]](this, "password", None)

  private val group = new ToggleGroup()
  private val usePasswordButton = new RadioButton {
    text = "Use a password"
    toggleGroup = group
    selected = true
    handleEvent(Event.ANY) { () => handlePasswordChange()  }
  }
  private val noPasswordButton = new RadioButton {
    text = "Don't use any password"
    toggleGroup = group
    handleEvent(Event.ANY) { () => handlePasswordChange() }
  }
  private val noPasswordProperty = noPasswordButton.selected

  private val passwordField = new PasswordField() {
    disable <== noPasswordProperty
    handleEvent(Event.ANY) { () => handlePasswordChange() }
  }
  private val repeatPasswordField = new PasswordField() {
    disable <== noPasswordProperty
    handleEvent(Event.ANY) { () => handlePasswordChange() }
  }
  private val passwordWarningLabel = new Label() {
    textFill = Color.web("#da4100")
    textAlignment = TextAlignment.CENTER
    alignment = Pos.TOP_CENTER
    prefHeight = 35
    disable <== noPasswordProperty
  }

  private val passwordPane = new GridPane {
    hgap = 5
    vgap = 5
    columnConstraints = Seq(
      new ColumnConstraints {
        minWidth = 130
        hgrow = Priority.NEVER
      },
      new ColumnConstraints { hgrow = Priority.ALWAYS }
    )
    padding = Insets(0, 0, 0, 20)
    add(new Label("Password") { disable <== noPasswordProperty }, 0, 0)
    add(passwordField, 1, 0)
    add(new Label("Repeat password") { disable <== noPasswordProperty }, 0, 1)
    add(repeatPasswordField, 1, 1)
    add(passwordWarningLabel, 0, 2, 2, 1)
  }

  content = new VBox(spacing = 5) {
    padding = Insets(10, 50, 10, 50)
    content = Seq(
      new Label("Choose a password") { styleClass.add("stepTitle") },
      new Label("You can use a password to protect the information that " +
         "Coinffeine saves in your computer.") {
        wrapText = true
      },
      new VBox() {
        padding = Insets(5, 10, 0, 10)
        content = Seq(usePasswordButton, passwordPane, noPasswordButton)
      }
    )
  }

  /** Translates updates on the password property to the setupConfig property */
  override def bindTo(setupConfig: ObjectProperty[SetupConfig]): Unit = {
    password.onChange {
      setupConfig.value = setupConfig.value.copy(password.value)
    }
  }

  private def handlePasswordChange(): Unit = {
    val pass1 = passwordField.text.value
    val pass2 = repeatPasswordField.text.value
    val passwordsMatch = pass1 == pass2
    password.value = if (noPasswordProperty.value) None else Some(pass1)
    canContinue.value = noPasswordProperty.value || (!pass1.isEmpty && passwordsMatch)
    passwordWarningLabel.text = Seq(
      if (!pass1.isEmpty && passwordValidator.isWeak(pass1)) Some("password is weak") else None,
      if (!pass2.isEmpty && !passwordsMatch) Some("passwords doesn't match") else None
    ).flatten.mkString(" and ")
  }
}
