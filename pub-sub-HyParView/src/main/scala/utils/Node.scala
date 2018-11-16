package utils

import java.net.InetSocketAddress

import akka.actor.ActorRef

case class Node(name: String, testAppActor: ActorRef, pubSubActor: ActorRef, gossipActor: ActorRef, membershipActor: ActorRef,
                address: InetSocketAddress) {
  override def toString = name
}

case class Start(node: Node)
