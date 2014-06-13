package com.coinffeine.gui.qrcode

import java.util.{HashMap => JavaHashMap}

import com.google.zxing.{BarcodeFormat, EncodeHintType}
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import scalafx.scene.image.{Image, PixelFormat, WritableImage}
import scalafx.scene.paint.Color

object QRCode {

  private val hints = new JavaHashMap[EncodeHintType, Any]() {
    put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L)
  }

  def encode(text: String, size: Int): Image = toWritableImage(toBitMatrix(text, size))

  private def toBitMatrix(text: String, size: Int): BitMatrix =
    new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)

  private def toWritableImage(bitMatrix: BitMatrix): WritableImage = {
    val image = new WritableImage(bitMatrix.getWidth, bitMatrix.getHeight)
    val writer = image.getPixelWriter
    val format = PixelFormat.getByteRgbInstance
    for (y <- 0 to (bitMatrix.getHeight - 1);
         x <- 0 to (bitMatrix.getWidth - 1)) {
      writer.setColor(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
    }
    image
  }
}
