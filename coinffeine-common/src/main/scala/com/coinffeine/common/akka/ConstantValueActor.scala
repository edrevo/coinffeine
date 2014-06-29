package com.coinffeine.common.akka

import akka.actor.Actor

import com.coinffeine.common.akka.ConstantValueActor.{UnsetValue, SetValue}

class ConstantValueActor() extends Actor {
  var response: Option[Any] = None
  override val receive: Receive = {
    case SetValue(v) => response = Some(v)
    case UnsetValue => response = None
    case _ => response.map(sender().!)
  }
}

/** Control messages for ConstantValueActor */
object ConstantValueActor {
  case class SetValue(v: Any)
  case object UnsetValue
}
