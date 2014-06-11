package com.coinffeine.gui

import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage

object Main extends JFXApp {
  stage = new PrimaryStage {
    title = "Coinffeine"
    scene = new MainScene
  }
}
