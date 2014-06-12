package com.coinffeine.gui.wizard

import scalafx.Includes._
import scalafx.beans.property.IntegerProperty
import scalafx.scene.layout.HBox
import scalafx.scene.paint.Color
import scalafx.scene.shape.Circle

/** A pane with a dot per step. The dot matching with the current step has a different color. */
private[wizard] class ProgressIndicator(steps: Int, currentStep: IntegerProperty) {

  val pane = new HBox(spacing = 5) {
    content = for (index <- 1 to steps) yield new Circle {
      radius = 5
      fill <== (when(currentStep === index)
        choose ProgressIndicator.SelectedColor
        otherwise ProgressIndicator.UnselectedColor)
    }
  }
}

private[wizard] object ProgressIndicator {
  private val SelectedColor = Color.RED
  private val UnselectedColor = Color.web("#afc9e1")
}

