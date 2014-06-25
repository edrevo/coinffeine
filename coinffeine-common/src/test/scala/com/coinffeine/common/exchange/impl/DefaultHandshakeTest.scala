package com.coinffeine.common.exchange.impl

import com.google.bitcoin.core.VerificationException

import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin.ImmutableTransaction
import com.coinffeine.common.exchange.Handshake.InvalidRefundTransaction

class DefaultHandshakeTest extends ExchangeTest {

  "The refund transaction" should "refund the right amount for the buyer" in new BuyerHandshake {
    valueSent(buyerHandshake.myUnsignedRefund, buyerWallet) should be (0.1.BTC)
  }

  it should "refund the right amount for the seller" in new SellerHandshake {
    valueSent(sellerHandshake.myUnsignedRefund, sellerWallet) should be (1.BTC)
  }

  it should "not be directly broadcastable to the blockchain" in new BuyerHandshake {
    a [VerificationException] should be thrownBy {
      sendToBlockChain(buyerHandshake.myUnsignedRefund.get)
    }
  }

  it should "not be broadcastable if locktime hasn't expired yet" in new BuyerHandshake {
    sendToBlockChain(buyerHandshake.myDeposit.get)
    a [VerificationException] should be thrownBy {
      sendToBlockChain(buyerHandshake.myUnsignedRefund.get)
    }
  }

  it should "not be broadcastable after locktime when unsigned" in new BuyerHandshake {
    sendToBlockChain(buyerHandshake.myDeposit.get)
    mineUntilLockTime(exchange.parameters.lockTime)
    a [VerificationException] should be thrownBy {
      sendToBlockChain(buyerHandshake.myUnsignedRefund.get)
    }
  }

  it should "be broadcastable after locktime if it has been signed" in
    new BuyerHandshake with SellerHandshake {
      val signature = sellerHandshake.signHerRefund(buyerHandshake.myUnsignedRefund)
      val signedBuyerRefund = buyerHandshake.signMyRefund(signature).get
      sendToBlockChain(buyerHandshake.myDeposit.get)
      mineUntilLockTime(exchange.parameters.lockTime)
      sendToBlockChain(signedBuyerRefund)
      balance(buyerWallet) should be (0.1.BTC)
    }

  "A handshake" should "reject signing counterpart deposit with a different lock time" in
    new BuyerHandshake with SellerHandshake {
      val depositWithWrongLockTime = ImmutableTransaction {
        val tx = buyerHandshake.myUnsignedRefund.get
        tx.setLockTime(exchange.parameters.lockTime - 10)
        tx
      }
      an [InvalidRefundTransaction] should be thrownBy {
        sellerHandshake.signHerRefund(depositWithWrongLockTime)
      }
    }

  it should "reject signing counterpart deposit with no lock time" in
    new BuyerHandshake with SellerHandshake {
      val depositWithNoLockTime = ImmutableTransaction {
        val tx = buyerHandshake.myUnsignedRefund.get
        tx.setLockTime(0)
        tx
      }
      an [InvalidRefundTransaction] should be thrownBy {
        sellerHandshake.signHerRefund(depositWithNoLockTime)
      }
    }

  it should "reject signing counterpart deposit with other than one input" in
    new BuyerHandshake with SellerHandshake {
      val depositWithoutInputs = ImmutableTransaction {
        val tx = buyerHandshake.myUnsignedRefund.get
        tx.clearInputs()
        tx
      }
      an [InvalidRefundTransaction] should be thrownBy {
        sellerHandshake.signHerRefund(depositWithoutInputs)
      }
    }
}
