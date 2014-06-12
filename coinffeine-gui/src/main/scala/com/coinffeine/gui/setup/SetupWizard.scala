package com.coinffeine.gui.setup

import com.coinffeine.gui.wizard.Wizard

/** Wizard to collect the initial configuration settings */
class SetupWizard extends Wizard[SetupConfig](
  title = "Initial setup",
  steps = Seq(new PasswordStepPane, new OkPayCredentialsStepPane, new TopUpStepPane),
  initialData = SetupConfig(password = None, okPayCredentials = None)
)
