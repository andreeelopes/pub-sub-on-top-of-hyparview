package gossip

import akka.actor.ActorRef


case class Gossip[A](mid: Array[Byte], message: A)

case class GossipDelivery[A](message: A)

case class Send[A](mid: Array[Byte], message: A)

case class Start(membershipActor: ActorRef, pubSubActor: ActorRef)

case class PassGossip[A](mid: Array[Byte], message: A)
