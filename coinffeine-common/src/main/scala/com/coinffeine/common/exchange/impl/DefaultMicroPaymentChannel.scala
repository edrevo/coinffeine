package com.coinffeine.common.exchange.impl

import com.coinffeine.common._
import com.coinffeine.common.bitcoin.{ImmutableTransaction, TransactionSignature}
import com.coinffeine.common.exchange._
import com.coinffeine.common.exchange.MicroPaymentChannel.{FinalStep, IntermediateStep, StepSignatures}
import com.coinffeine.common.exchange.impl.DefaultMicroPaymentChannel._

private[impl] class DefaultMicroPaymentChannel[C <: FiatCurrency](
    role: Role,
    exchange: Exchange[C],
    deposits: Deposits,
    override val currentStep: MicroPaymentChannel.Step = IntermediateStep(1))
  extends MicroPaymentChannel[C] {

  private val requiredSignatures = Seq(exchange.buyer.bitcoinKey, exchange.seller.bitcoinKey)

  private val currentUnsignedTransaction = ImmutableTransaction {
    import exchange.amounts._

    val buyerOutput = currentStep match {
      case FinalStep => bitcoinAmount + buyerDeposit
      case IntermediateStep(i) => stepBitcoinAmount * (i - 1)
    }
    val sellerOutput = currentStep match {
      case FinalStep => sellerDeposit - bitcoinAmount
      case IntermediateStep(i) => bitcoinAmount - stepBitcoinAmount * (i - 1)
    }

    TransactionProcessor.createUnsignedTransaction(
      inputs = deposits.toSeq.map(_.get.getOutput(0)),
      outputs = Seq(
        exchange.buyer.bitcoinKey -> buyerOutput,
        exchange.seller.bitcoinKey -> sellerOutput
      ),
      network = exchange.parameters.network
    )
  }

  override def validateCurrentTransactionSignatures(herSignatures: StepSignatures): Boolean = {
    val tx = currentUnsignedTransaction.get
    val herKey = role.her(exchange).bitcoinKey

    def isValid(index: Int, signature: TransactionSignature) =
      TransactionProcessor.isValidSignature(tx, index, signature, herKey, requiredSignatures)

    isValid(BuyerDepositInputIndex, herSignatures.buyerDepositSignature) &&
      isValid(SellerDepositInputIndex, herSignatures.sellerDepositSignature)
  }

  override def signCurrentTransaction = {
    val tx = currentUnsignedTransaction.get
    val signingKey = role.me(exchange).bitcoinKey
    StepSignatures(
      buyerDepositSignature = TransactionProcessor.signMultiSignedOutput(
        tx, BuyerDepositInputIndex, signingKey, requiredSignatures),
      sellerDepositSignature = TransactionProcessor.signMultiSignedOutput(
        tx, SellerDepositInputIndex, signingKey, requiredSignatures)
    )
  }

  override def nextStep: DefaultMicroPaymentChannel[C] = {
    val nextStep = currentStep match {
      case FinalStep => throw new IllegalArgumentException("Already at the last step")
      case IntermediateStep(exchange.parameters.breakdown.intermediateSteps) => FinalStep
      case IntermediateStep(i) => IntermediateStep(i + 1)
    }
    new DefaultMicroPaymentChannel[C](role, exchange, deposits, nextStep)
  }

  override def closingTransaction(herSignatures: StepSignatures) = {
    val tx = currentUnsignedTransaction.get
    val signatures = Seq(signCurrentTransaction, herSignatures)
    val buyerSignatures = signatures.map(_.buyerDepositSignature)
    val sellerSignatures = signatures.map(_.sellerDepositSignature)
    TransactionProcessor.setMultipleSignatures(tx, BuyerDepositInputIndex, buyerSignatures: _*)
    TransactionProcessor.setMultipleSignatures(tx, SellerDepositInputIndex, sellerSignatures: _*)
    ImmutableTransaction(tx)
  }
}

private[impl] object DefaultMicroPaymentChannel {
  val BuyerDepositInputIndex = 0
  val SellerDepositInputIndex = 1
}
