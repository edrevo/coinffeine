package com.bitwise.bitmarket.protorpc

import java.util.Currency

import akka.actor.{ActorRef, Props}

object ProtobufServerActor {
  trait Component {
    def protobufServerActorProps(port: Int, brokers: Map[Currency, ActorRef]): Props
  }
}
