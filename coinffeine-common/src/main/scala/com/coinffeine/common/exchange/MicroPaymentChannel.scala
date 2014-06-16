package com.coinffeine.common.exchange

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.exchange.impl.DefaultClosing

abstract class MicroPaymentChannel[C <: FiatCurrency](val exchange: Exchange[C]) {

  def deposits: Deposits
  def currentStep: Exchange.StepNumber
  def currentTransaction: exchange.Transaction

  def buyerFundsAfterCurrentStep = exchange.amounts.buyerFundsAfter(currentStep)
  def sellerFundsAfterCurrentStep = exchange.amounts.sellerFundsAfter(currentStep)
  def isLastStep = exchange.amounts.totalSteps.isLastStep(currentStep)

  def validateCurrentTransactionSignatures(
    buyerSignature: exchange.TransactionSignature,
    sellerSignature: exchange.TransactionSignature): Boolean

  def signCurrentTransaction: exchange.TransactionSignature

  def nextStep: MicroPaymentChannel[C]

  def close(): Closing[C]
}
