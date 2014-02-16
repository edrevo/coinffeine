package com.coinffeine.system

import akka.actor.Props

trait SupervisorComponent {

  /** Properties of system supervisor actor given the command line arguments */
  def supervisorProps(args: Array[String]): Props
}
