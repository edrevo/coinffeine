package com.coinffeine.common.exchange.impl

import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin.ImmutableTransaction

class DefaultHandshakeTest extends ExchangeTest {

  "A handshake" should "create a refund of the right amount for the buyer" in new BuyerHandshake {
    valueSent(buyerHandshake.myUnsignedRefund, buyerWallet) should be (0.1.BTC)
  }

  it should "create a refund of the right amount for the seller" in new SellerHandshake {
    valueSent(sellerHandshake.myUnsignedRefund, sellerWallet) should be (1.BTC)
  }

  it should "produce a signature for the counterpart to get a refund" in
    new BuyerHandshake with SellerHandshake {
      val signature = sellerHandshake.signHerRefund(buyerHandshake.myUnsignedRefund)
      val signedBuyerRefund = buyerHandshake.signMyRefund(signature).get
      sendToBlockChain(buyerHandshake.myDeposit.get)
      mineUntilLockTime(exchange.parameters.lockTime)
      sendToBlockChain(signedBuyerRefund)
      balance(buyerWallet) should be (0.1.BTC)
    }

  it should "reject signing counterpart deposit with a different lock time" in
    new BuyerHandshake with SellerHandshake {
      val depositWithWrongLockTime = ImmutableTransaction {
        val tx = buyerHandshake.myUnsignedRefund.get
        tx.setLockTime(exchange.parameters.lockTime - 10)
        tx
      }
      an [sellerHandshake.InvalidRefundTransaction] should be thrownBy {
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
      an [sellerHandshake.InvalidRefundTransaction] should be thrownBy {
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
      an [sellerHandshake.InvalidRefundTransaction] should be thrownBy {
        sellerHandshake.signHerRefund(depositWithoutInputs)
      }
    }
}
