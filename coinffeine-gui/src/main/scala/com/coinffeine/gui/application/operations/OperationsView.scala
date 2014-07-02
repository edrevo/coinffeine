package com.coinffeine.gui.application.operations

import scala.util.Try

import scalafx.beans.property.BooleanProperty
import scalafx.collections.ObservableBuffer
import scalafx.event.{ActionEvent, Event}
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control._
import scalafx.scene.layout._

import com.coinffeine.gui.application.ApplicationView

class OperationsView extends ApplicationView {

  import com.coinffeine.gui.application.operations.OperationsView._

  private val amountTextField = new TextField {
    alignment = Pos.CENTER_RIGHT
    prefWidth = 100
    text = "0.00"
  }

  private val amountIsValid = new BooleanProperty(this, "AmountIsValid", false)

  private val priceRadioButtonGroup = new ToggleGroup()

  private val marketPriceRadioButton = new RadioButton {
    text = "At market price"
    selected = true
    toggleGroup = priceRadioButtonGroup
  }

  private val customPriceRadioButton = new RadioButton {
    text = "At custom price (not supported yet)"
    selected = false
    disable = true // disabled until custom price is supported
    toggleGroup = priceRadioButtonGroup
  }

  private val customPriceSelectedProperty = customPriceRadioButton.selected

  amountTextField.handleEvent(Event.ANY) { () => handleSubmitButtonEnabled() }

  private def handleSubmitButtonEnabled(): Unit = {
    amountIsValid.value =  Try(amountTextField.text.getValueSafe.toDouble > 0.0).getOrElse(false)
  }

  override def name: String = "Operations"

  override def centerPane: Pane = new StackPane {
    content = new VBox {
      alignment = Pos.CENTER
      prefWidth = 500
      minWidth = 500
      prefHeight = 300
      minHeight = 300
      padding = Insets(50)
      spacing = 20
      content = Seq(
        new HBox {
          alignment = Pos.CENTER_LEFT
          spacing = 10
          content = Seq(
            new Label("I want to"),
            new ChoiceBox[Operation] {
              items = ObservableBuffer(Seq(BuyOperation, SellOperation))
              value = BuyOperation
              prefWidth = 80
            },
            amountTextField,
            new Label("BTCs using"),
            new ChoiceBox[PaymentProcessor] {
              items = ObservableBuffer(Seq(OKPay))
              value = OKPay
              prefWidth = 80
            }
          )
        },
        new VBox {
          spacing = 20
          content = Seq(
            customPriceRadioButton,
            new GridPane {
              hgap = 10
              vgap = 10
              disable <== customPriceSelectedProperty.not()
              add(rightAlignedLabel("X"), 0, 0)
              add(new TextField { text = "0.00" }, 1, 0)
              add(rightAlignedLabel("€"), 2, 0)

              add(new Label("Limit"), 0, 1)
              add(new TextField { text = "0.00" }, 1, 1)
              add(new Label("€"), 2, 1)
            },
            marketPriceRadioButton
          )
        },
        new Button {
          text = "Submit"
          disable <== amountIsValid.not()
          handleEvent(ActionEvent.ACTION) { () =>
            // TODO: submit the order
          }
        }
      )
    }
  }

  private def rightAlignedLabel(labelText: String) = new Label {
    text = labelText
    alignmentInParent = Pos.CENTER_RIGHT
  }
}

object OperationsView {
  sealed trait Operation {
    override def toString = name
    def name: String
  }
  case object BuyOperation extends Operation {
    override val name: String = "BUY"
  }
  case object SellOperation extends Operation {
    override val name: String = "SELL"
  }

  sealed trait PaymentProcessor {
    override def toString = name
    def name: String
  }
  case object OKPay extends PaymentProcessor {
    override val name = "OKPay"
  }
}
