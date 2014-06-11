package com.coinffeine.gui

import javafx.scene.Scene
import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafxml.core.{FXMLView, NoDependencyResolver}

object Main extends JFXApp {
  val root = FXMLView(getClass.getResource("main.fxml"), NoDependencyResolver)
  stage = new PrimaryStage {
    title = "ScalaFX Hello World"
    scene = new Scene(root)
  }
}
