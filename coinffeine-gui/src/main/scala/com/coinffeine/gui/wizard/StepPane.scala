package com.coinffeine.gui.wizard

import scalafx.beans.property.ObjectProperty
import scalafx.scene.layout.Pane

/** An step of a wizard */
trait StepPane[Data] extends Pane {
  def bindTo(data: ObjectProperty[Data]): Unit
}
