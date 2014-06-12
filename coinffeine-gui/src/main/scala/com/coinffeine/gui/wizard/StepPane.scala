package com.coinffeine.gui.wizard

import scalafx.beans.property.{BooleanProperty, ObjectProperty}
import scalafx.scene.layout.Pane

/** An step of a wizard */
trait StepPane[Data] extends Pane {
  def bindTo(data: ObjectProperty[Data]): Unit
  val canContinue: BooleanProperty = new BooleanProperty(this, "canContinue", false)
}
