package com.coinffeine.gui.setup

import com.coinffeine.gui.wizard.Wizard

/** Wizard to collect the initial configuration settings */
class SetupWizard(walletAddress: String, validator: CredentialsValidator) extends Wizard[SetupConfig](
  title = "Initial setup",
  steps = Seq(
    new PasswordStepPane,
    new OkPayCredentialsStepPane(validator),
    new TopUpStepPane(walletAddress)
  ),
  initialData = SetupConfig(password = None, okPayCredentials = None)
)
