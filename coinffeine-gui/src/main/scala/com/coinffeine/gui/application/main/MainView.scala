package com.coinffeine.gui.application.main

import scalafx.geometry.Insets
import scalafx.scene.control.Label
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{Pane, StackPane}
import scalafx.scene.text.Font

import com.coinffeine.gui.application.ApplicationView

/** Initial application view */
class MainView extends ApplicationView {

  /** Name of the view */
  override val name: String = "Main"

  /** Pane to be displayed at the center area of the application when this view is active */
  override val centerPane: Pane = new StackPane {
    prefWidth = 200
    prefHeight = 150
    content = Seq(
      new ImageView(new Image(getClass.getResourceAsStream("/graphics/logo-128x128.png"))) {
        preserveRatio = true
        margin = Insets(top = 0, right = 0, bottom = 40, left = 0)
      },
      new Label("Welcome to Coinffeine") {
        font = Font(18)
        margin = Insets(top = 128, right = 0, bottom = 0, left = 0)
      }
    )
  }
}
