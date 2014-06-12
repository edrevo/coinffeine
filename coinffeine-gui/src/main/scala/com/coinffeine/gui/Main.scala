package com.coinffeine.gui

import com.coinffeine.gui.setup.SetupWizard

import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage

object Main extends JFXApp {
  JFXApp.AUTO_SHOW = false

  val setupConfig = new SetupWizard().show()

  stage = new PrimaryStage {
    title = "Coinffeine"
    scene = new MainScene
  }
  stage.show()
}
