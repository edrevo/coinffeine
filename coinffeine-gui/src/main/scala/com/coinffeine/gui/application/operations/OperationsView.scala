package com.coinffeine.gui.application.operations

import scalafx.scene.control.Label
import scalafx.scene.layout.{Pane, StackPane}

import com.coinffeine.gui.application.ApplicationView

class OperationsView extends ApplicationView {

  override def name: String = "Operations"

  override def centerPane: Pane = new StackPane {
    content = Seq(new Label("Operations"))
  }
}
