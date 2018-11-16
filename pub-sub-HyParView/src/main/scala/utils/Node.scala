package utils

import akka.actor.ActorRef

case class Node(name:String, testAppActor: ActorRef, pubSubActor: ActorRef, gossipActor: ActorRef, membershipActor: ActorRef) {
  override def toString = name
}

case class Start(node: Node)