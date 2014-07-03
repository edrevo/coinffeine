package com.coinffeine.gui.control

import javafx.beans.value.{ChangeListener, ObservableValue}
import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.scene.control.TextField
import scalafx.scene.input.KeyEvent

class DecimalNumberTextField(initialValue: BigDecimal) extends TextField {

  require(initialValue >= 0, "Initial value cannot be negative")

  text = initialValue.toString

  /*
   * The filter event is needed regardless we have a listener that checks the input. Once the
   * key type event is processed, the caret is moved one character to the right. We have to
   * filter the invalid characters to avoid such a behaviour.
   */
  filterEvent(KeyEvent.KeyTyped) { (event: KeyEvent) =>
    if (!isValidInputChar(event.character)) {
      event.consume()
    }
  }

  /*
   * This listener is used to ignore any invalid value set on the text property by replacing
   * it by the previous one.
   */
  text.addListener(new ChangeListener[String] {

    private var ignore = false

    private def ignoringSelfChanges(action: => Unit): Unit = {
      ignore = true
      action
      ignore = false
    }

    override def changed(observable: ObservableValue[_ <: String],
                         oldValue: String,
                         newValue: String): Unit = {
      if (!ignore && !isValidInput(newValue)) {
        ignoringSelfChanges {
          text = oldValue
        }
      }
    }
  })

  /*
   * This event handler is triggered when enter key is pressed, normalising the text.
   */
  handleEvent(ActionEvent.ACTION) { () =>
    text = normalise(text.value)
  }

  /*
   * This listener watches the focused property, which is changed when the control receives or
   * loses the focus. When that happens, the text is normalised.
   */
  focused.addListener { (observable: ObservableValue[_ <: java.lang.Boolean],
                       oldValue: java.lang.Boolean,
                       newValue: java.lang.Boolean) =>
    text = normalise(text.value)
  }

  private def isValidInputChar(input: String): Boolean = input match {
    case NumberRegex() => true
    case DotRegex() if !text.value.contains(".") => true
    case _ => false
  }

  private def isValidInput(input: String): Boolean = input match {
    case DecimalRegex() => true
    case _ => false
  }

  private def normalise(input: String): String =
    BigDecimal(if (input.isEmpty) "0" else input).toString

  private val DecimalRegex = "\\d*\\.?\\d*".r
  private val NumberRegex = "\\d".r
  private val DotRegex = "\\.".r
}
