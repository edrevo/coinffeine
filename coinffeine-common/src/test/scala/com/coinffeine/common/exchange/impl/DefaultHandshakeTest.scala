package com.coinffeine.common.exchange.impl

import com.coinffeine.common.BitcoinjTest
import com.coinffeine.common.Currency.Bitcoin
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin.ImmutableTransaction
import com.coinffeine.common.exchange.{BuyerRole, SellerRole}
import com.coinffeine.common.exchange.Exchange.UnspentOutput

class DefaultHandshakeTest extends BitcoinjTest {

  import com.coinffeine.common.exchange.impl.Samples.exchange

  val protocol = new DefaultExchangeProtocol()

  "A handshake" should "create a refund of the right amount for the buyer" in new BuyerHandshake {
    Bitcoin.fromSatoshi(buyerHandshake.myUnsignedRefund.get.getValueSentToMe(buyerWallet)) should be (0.1.BTC)
  }

  it should "create a refund of the right amount for the seller" in new SellerHandshake {
    Bitcoin.fromSatoshi(sellerHandshake.myUnsignedRefund.get.getValueSentToMe(sellerWallet)) should be (1.BTC)
  }

  it should "produce a signature for the counterpart to get a refund" in
    new BuyerHandshake with SellerHandshake {
      val signature = sellerHandshake.signHerRefund(buyerHandshake.myUnsignedRefund)
      val signedBuyerRefund = buyerHandshake.signMyRefund(signature).get
      sendToBlockChain(buyerHandshake.myDeposit.get)
      while(signedBuyerRefund.getLockTime > blockStore.getChainHead.getHeight) {
        mineBlock()
      }
      sendToBlockChain(signedBuyerRefund)
      Bitcoin.fromSatoshi(buyerWallet.getBalance) should be (0.1.BTC)
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

  it should "create a micropayment channel" in new BuyerHandshake with SellerHandshake {
    val channel = buyerHandshake.createMicroPaymentChannel(sellerHandshake.myDeposit)
    channel.deposits.buyerDeposit.get should be (buyerHandshake.myDeposit.get)
    channel.deposits.sellerDeposit.get should be (sellerHandshake.myDeposit.get)
  }

  trait BuyerHandshake {
    val buyerWallet = createWallet(exchange.buyer.bitcoinKey, 0.2.BTC)
    val buyerFunds = TransactionProcessor.collectFunds(buyerWallet, 0.2.BTC)
      .toSeq.map(UnspentOutput(_, exchange.buyer.bitcoinKey))
    val buyerHandshake =
      protocol.createHandshake(exchange, BuyerRole, buyerFunds, buyerWallet.getChangeAddress)
  }

  trait SellerHandshake {
    val sellerWallet = createWallet(exchange.seller.bitcoinKey, 1.1.BTC)
    val sellerFunds = TransactionProcessor.collectFunds(sellerWallet, 0.2.BTC)
      .toSeq.map(UnspentOutput(_, exchange.seller.bitcoinKey))
    val sellerHandshake =
      protocol.createHandshake(exchange, SellerRole, sellerFunds, sellerWallet.getChangeAddress)
  }
}
