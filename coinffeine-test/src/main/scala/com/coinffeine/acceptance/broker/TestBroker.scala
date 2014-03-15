package com.coinffeine.acceptance.broker

import akka.actor.Props

import com.coinffeine.common.PeerConnection
import com.coinffeine.acceptance.TestActorSystem

/** Testing façade for a Coinffeine broker */
class TestBroker(supervisorProps: Props, port: Int)
    extends TestActorSystem(supervisorProps, s"broker$port") {

  val address = PeerConnection("localhost", port)
}
