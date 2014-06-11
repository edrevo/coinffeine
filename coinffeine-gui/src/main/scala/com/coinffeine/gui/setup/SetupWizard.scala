package com.coinffeine.gui.setup

import com.coinffeine.gui.wizard.Wizard

/** Wizard to collect the initial configuration settings */
class SetupWizard extends Wizard[SetupConfig](
  title = "Initial setup",
  steps = Seq(new PasswordStep, new OkPayCredentialsStep, new TopUpStep),
  initialData = SetupConfig(password = None, okPayCredentials = None)
)
