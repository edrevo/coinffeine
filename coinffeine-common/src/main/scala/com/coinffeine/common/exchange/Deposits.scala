package com.coinffeine.common.exchange

case class Deposits[Transaction](buyerDeposit: Transaction, sellerDeposit: Transaction)
