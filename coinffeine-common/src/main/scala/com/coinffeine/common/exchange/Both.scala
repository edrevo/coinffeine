package com.coinffeine.common.exchange

/** Utility class for a pair of values belonging to buyer and seller respectively.
  *
  * This class follows the convention for the buyer-seller ordering. Use only with immutable
  * classes.
  */
case class Both[T](buyer: T, seller: T) {

  /** Access to a value by role */
  def apply(role: Role): T = role match {
    case BuyerRole => buyer
    case SellerRole => seller
  }

  def map[S](f: T => S): Both[S] = Both(
    buyer = f(buyer),
    seller = f(seller)
  )

  def forall(pred: T => Boolean): Boolean = pred(buyer) && pred(seller)

  def roleOf(value: T): Option[Role] = value match {
    case `buyer` => Some(BuyerRole)
    case `seller` => Some(SellerRole)
    case _ => None
  }

  def updated(role: Role, value: T): Both[T] = role match {
    case BuyerRole => copy(buyer = value)
    case SellerRole => copy(seller = value)
  }

  def toSet: Set[T] = Set(buyer, seller)

  def toSeq: Seq[T] = Seq(buyer, seller)

  def toTuple: (T, T) = (buyer, seller)

  def swap: Both[T] = Both(seller, buyer)
}

object Both {
  def fill[T](value: T): Both[T] = Both(value, value)
}
