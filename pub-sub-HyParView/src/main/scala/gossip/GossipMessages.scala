package gossip

import akka.actor.ActorRef


case class Gossip[E](mid: Array[Byte], message: E)

case class GossipDelivery[E](message: E)

case class Send[E](mid: Array[Byte], message: E)

case class Start(membershipActor: ActorRef, pubSubActor: ActorRef)