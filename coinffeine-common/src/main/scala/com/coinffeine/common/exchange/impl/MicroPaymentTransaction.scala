package com.coinffeine.common.exchange.impl

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.exchange.{Deposits, Exchange}

class MicroPaymentTransaction(
    exchange: DefaultExchange[_ <: FiatCurrency],
    deposits: Deposits[ImmutableTransaction],
    currentStep: Exchange.StepNumber) extends ImmutableTransaction({

  val depositLost = !exchange.amounts.totalSteps.isLastStep(currentStep)

  val buyerFundsAfter = {
    val (stepFunds, _) = exchange.amounts.buyerFundsAfter(currentStep)
    if (depositLost) stepFunds else stepFunds + exchange.amounts.buyerDeposit
  }

  val sellerFundsAfter = {
    val (stepFunds, _) = exchange.amounts.sellerFundsAfter(currentStep)
    if (depositLost) stepFunds else stepFunds + exchange.amounts.sellerDeposit
  }

  val transactionInputs = Seq(
    deposits.buyerDeposit.get.getOutput(0),
    deposits.sellerDeposit.get.getOutput(0)
  )

  TransactionProcessor.createUnsignedTransaction(
    inputs = transactionInputs,
    outputs = Seq(
      exchange.buyer.bitcoinKey -> buyerFundsAfter,
      exchange.seller.bitcoinKey -> sellerFundsAfter),
    network = exchange.parameters.network
  )
})
