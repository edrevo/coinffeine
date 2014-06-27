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

  def toSet: Set[T] = Set(buyer, seller)

  def toSeq: Seq[T] = Seq(buyer, seller)

  def toTuple: (T, T) = (buyer, seller)

  def swap: Both[T] = Both(seller, buyer)
}
