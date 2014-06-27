package com.coinffeine.common.exchange.impl

import scala.util.Try

import com.coinffeine.common._
import com.coinffeine.common.bitcoin.{ImmutableTransaction, TransactionSignature}
import com.coinffeine.common.exchange._
import com.coinffeine.common.exchange.MicroPaymentChannel.{FinalStep, IntermediateStep, Signatures}
import com.coinffeine.common.exchange.impl.DefaultMicroPaymentChannel._

private[impl] class DefaultMicroPaymentChannel(
    role: Role,
    exchange: Exchange[_ <: FiatCurrency],
    deposits: Deposits,
    override val currentStep: MicroPaymentChannel.Step = IntermediateStep(1))
  extends MicroPaymentChannel {

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

  override def validateCurrentTransactionSignatures(herSignatures: Signatures): Try[Unit] = {
    val tx = currentUnsignedTransaction.get
    val herKey = role.her(exchange).bitcoinKey

    def requireValidSignature(index: Int, signature: TransactionSignature) = {
      require(
        TransactionProcessor.isValidSignature(tx, index, signature, herKey, requiredSignatures),
        s"Signature $signature cannot satisfy ${tx.getInput(index)}"
      )
    }

    Try {
      requireValidSignature(BuyerDepositInputIndex, herSignatures.buyerDepositSignature)
      requireValidSignature(SellerDepositInputIndex, herSignatures.sellerDepositSignature)
    }
  }

  override def signCurrentTransaction = {
    val tx = currentUnsignedTransaction.get
    val signingKey = role.me(exchange).bitcoinKey
    Signatures(
      buyerDepositSignature = TransactionProcessor.signMultiSignedOutput(
        tx, BuyerDepositInputIndex, signingKey, requiredSignatures),
      sellerDepositSignature = TransactionProcessor.signMultiSignedOutput(
        tx, SellerDepositInputIndex, signingKey, requiredSignatures)
    )
  }

  override def nextStep: DefaultMicroPaymentChannel = {
    val nextStep = currentStep match {
      case FinalStep => throw new IllegalArgumentException("Already at the last step")
      case IntermediateStep(exchange.parameters.breakdown.intermediateSteps) => FinalStep
      case IntermediateStep(i) => IntermediateStep(i + 1)
    }
    new DefaultMicroPaymentChannel(role, exchange, deposits, nextStep)
  }

  override def closingTransaction(herSignatures: Signatures) = {
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
