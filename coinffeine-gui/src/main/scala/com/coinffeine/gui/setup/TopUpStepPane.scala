package com.coinffeine.gui.setup

import java.net.URI

import scalafx.beans.property.ObjectProperty
import scalafx.event.ActionEvent
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Hyperlink, Label, TextField}
import scalafx.scene.image.ImageView
import scalafx.scene.layout.{HBox, Priority, StackPane, VBox}
import scalafx.scene.text.Font

import com.coinffeine.gui.qrcode.QRCode
import com.coinffeine.gui.wizard.StepPane

private[setup] class TopUpStepPane(address: String) extends StackPane with StepPane[SetupConfig] {

  content = new VBox(spacing = 5) {
    padding = Insets(10, 50, 10, 50)
    content = Seq(
      new Label("Add bitcoins to your Coinffeine wallet") { styleClass = Seq("stepTitle") },
      new HBox {
        alignment = Pos.BASELINE_LEFT
        content = Seq(
          new Label("You need a small amount of bitcoins to buy bitcoins."),
          new Hyperlink("Know why") {
            handleEvent(ActionEvent.ACTION) { () => openFAQ() }
          }
        )
      },
      new HBox(spacing = 10) {
        alignment = Pos.CENTER
        content = Seq(
          new VBox(spacing = 5) {
            alignment = Pos.CENTER
            hgrow = Priority.ALWAYS
            content = Seq(
              new Label("Your wallet address"),
              new TextField {
                id = "address"
                text = address
                editable = false
                font = Font(12)
              }
            )
          },
          new ImageView(QRCode.encode(s"bitcoin:$address", 150))
        )
      }
    )
  }

  override def bindTo(data: ObjectProperty[SetupConfig]): Unit = {
    canContinue.value = true
  }

  private def openFAQ(): Unit = {
    java.awt.Desktop.getDesktop.browse(TopUpStepPane.FaqUrl)
  }
}

private[setup] object TopUpStepPane {
  val FaqUrl = new URI("http://www.coinffeine.com/faq.html")
}
