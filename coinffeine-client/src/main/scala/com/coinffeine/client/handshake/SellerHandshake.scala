package com.coinffeine.client.handshake

import com.coinffeine.client.ExchangeInfo
import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.Wallet

class SellerHandshake[C <: FiatCurrency](exchangeInfo: ExchangeInfo[C], userWallet: Wallet)
  extends DefaultHandshake(
    exchangeInfo,
    amountToCommit = exchangeInfo.parameters.bitcoinAmount + exchangeInfo.btcStepAmount,
    userWallet)
