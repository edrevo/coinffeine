package com.coinffeine.common.exchange.impl

import com.google.bitcoin.crypto.TransactionSignature

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.exchange.{Deposits, Exchange, MicroPaymentChannel, Role}
import com.coinffeine.common.exchange.impl.DefaultMicroPaymentChannel._

case class DefaultMicroPaymentChannel[C <: FiatCurrency](
    role: Role,
    override val exchange: DefaultExchange[C],
    deposits: Deposits[ImmutableTransaction],
    override val currentStep: Exchange.StepNumber = Exchange.StepNumber.First)
  extends MicroPaymentChannel[C](exchange) {

  private val currentUnsignedTransaction = ImmutableTransaction {
    TransactionProcessor.createUnsignedTransaction(
      inputs = Seq(
        deposits.buyerDeposit.get.getOutput(0),
        deposits.sellerDeposit.get.getOutput(0)
      ),
      outputs = Seq(
        exchange.buyer.bitcoinKey -> exchange.amounts.channelOutputForBuyerAfter(currentStep),
        exchange.seller.bitcoinKey -> exchange.amounts.channelOutputForSellerAfter(currentStep)
      ),
      network = exchange.parameters.network
    )
  }

  override def validateCurrentTransactionSignatures(herSignatures: StepSignatures): Boolean = {
    val tx = currentUnsignedTransaction.get

    def isValid(index: Int, signature: TransactionSignature) =
      TransactionProcessor.isValidSignature(tx, index, signature, role.her(exchange).bitcoinKey,
        Seq(exchange.buyer.bitcoinKey, exchange.seller.bitcoinKey))

    isValid(BuyerDepositInputIndex, herSignatures.buyerDepositSignature) &&
      isValid(SellerDepositInputIndex, herSignatures.sellerDepositSignature)
  }

  override def signCurrentTransaction: StepSignatures = ???

  override def nextStep: DefaultMicroPaymentChannel[C] = copy(currentStep = currentStep.next)

  override def closingTransaction(herSignatures: StepSignatures): exchange.Transaction = ???
}

object DefaultMicroPaymentChannel {
  val BuyerDepositInputIndex = 0
  val SellerDepositInputIndex = 1
}
