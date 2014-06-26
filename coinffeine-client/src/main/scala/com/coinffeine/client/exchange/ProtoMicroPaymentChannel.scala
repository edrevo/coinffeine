package com.coinffeine.client.exchange

import scala.util.Try

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.{MutableTransaction, TransactionSignature}
import com.coinffeine.common.exchange.MicroPaymentChannel.StepSignatures

trait ProtoMicroPaymentChannel[C <: FiatCurrency] {
  /** Returns the bitcoin transaction that corresponds with the offer for the passed in step */
  def getOffer(step: Int): MutableTransaction

  /** Returns a signed transaction ready to be broadcast */
  def getSignedOffer(step: Int, counterpartSignatures: StepSignatures): MutableTransaction

  /** Returns the transaction signature for the corresponding step */
  def signStep(step: Int): StepSignatures = sign(getOffer(step))

  /** Returns the bitcoin transaction that corresponds with the final offer */
  def finalOffer: MutableTransaction

  /** Returns the transaction signature for the final step */
  def finalSignatures: StepSignatures = sign(finalOffer)

  /** Validates that the signatures are valid for the offer in the passed in step */
  def validateSellersSignature(
    step: Int, signature0: TransactionSignature, signature1: TransactionSignature): Try[Unit]

  /** Validates that the signatures are valid for the final offer */
  def validateSellersFinalSignature(
    signature0: TransactionSignature, signature1: TransactionSignature): Try[Unit]

  /** Returns the user's signature for the transaction */
  protected def sign(offer: MutableTransaction): StepSignatures
}
