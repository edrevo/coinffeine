package com.coinffeine.gui.application.operations

import scala.util.Try

import scalafx.beans.property.BooleanProperty
import scalafx.collections.ObservableBuffer
import scalafx.event.{ActionEvent, Event}
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control._
import scalafx.scene.layout._

import com.coinffeine.common.Currency.Euro
import com.coinffeine.common.{CurrencyAmount, BitcoinAmount, Currency}
import com.coinffeine.gui.application.ApplicationView
import com.coinffeine.gui.control.DecimalNumberTextField

class OperationsView(onSubmit: OperationsView.FormData => Unit) extends ApplicationView {

  import com.coinffeine.gui.application.operations.OperationsView._

  private val operationChoiceBox = new ChoiceBox[Operation] {
    items = ObservableBuffer(Seq(BuyOperation, SellOperation))
    value = BuyOperation
    prefWidth = 80
  }

  private val amountTextField = new DecimalNumberTextField(0.0) {
    id = "amount"
    alignment = Pos.CENTER_RIGHT
    prefWidth = 100
  }

  private def bitcoinAmount: Try[BitcoinAmount] = Try {
    Currency.Bitcoin(BigDecimal(amountTextField.text.getValueSafe))
  }

  private def limitAmount: Try[CurrencyAmount[Euro.type]] = Try {
    Currency.Euro(BigDecimal(limitTextField.text.getValueSafe))
  }

  private val amountIsValid = new BooleanProperty(this, "AmountIsValid", false)
  private val limitIsValid = new BooleanProperty(this, "LimitIsValid", false)

  private val paymentProcessorChoiceBox = new ChoiceBox[PaymentProcessor] {
    items = ObservableBuffer(Seq(OKPay))
    value = OKPay
    prefWidth = 80
  }

  private val priceRadioButtonGroup = new ToggleGroup()

  private val marketPriceRadioButton = new RadioButton {
    text = "Market price order (not supported yet)"
    selected = false
    disable = true // disabled until market price is supported
    toggleGroup = priceRadioButtonGroup
  }

  private val limitOrderRadioButton = new RadioButton {
    text = "Limit order"
    selected = true
    toggleGroup = priceRadioButtonGroup
  }

  private val limitTextField = new DecimalNumberTextField(0.0) {
    id = "limit"
    alignment = Pos.CENTER_RIGHT
    prefWidth = 100
  }

  private val limitOrderSelectedProperty = limitOrderRadioButton.selected

  amountTextField.handleEvent(Event.ANY) { () => handleSubmitButtonEnabled() }
  limitTextField.handleEvent(Event.ANY) { () => handleSubmitButtonEnabled() }

  private def handleSubmitButtonEnabled(): Unit = {
    amountIsValid.value = bitcoinAmount.map(_.isPositive).getOrElse(false)
    limitIsValid.value = limitAmount.map(_.isPositive).getOrElse(false)
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
            operationChoiceBox,
            amountTextField,
            new Label("BTCs using"),
            paymentProcessorChoiceBox
          )
        },
        new VBox {
          spacing = 20
          content = Seq(
            new VBox {
              spacing = 20
              content = Seq(
                limitOrderRadioButton,
                new HBox {
                  spacing = 10
                  alignment = Pos.CENTER_LEFT
                  margin = Insets(0, 0, 0, 30)
                  disable <== limitOrderSelectedProperty.not()
                  content = Seq(new Label("Limit"), limitTextField, new Label("€"))
                }
              )
            },
            marketPriceRadioButton
          )
        },
        new Button {
          id = "submit"
          text = "Submit"
          disable <== amountIsValid.not() or limitIsValid.not()
          handleEvent(ActionEvent.ACTION) { () =>
            val data = FormData(
              operation = operationChoiceBox.value.value,
              amount = bitcoinAmount.get,
              limit = limitAmount.get,
              paymentProcessor = paymentProcessorChoiceBox.value.value
            )
            onSubmit(data)
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

  case class FormData(
    operation: Operation,
    amount: BitcoinAmount,
    limit: CurrencyAmount[Euro.type],
    paymentProcessor: PaymentProcessor)
}
