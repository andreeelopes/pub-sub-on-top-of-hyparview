package membership

import akka.actor.ActorRef

case class Join(newNode: ActorRef)

case class ForwardJoin(newNode: ActorRef, ttl: Long)

object Disconnect

case class Gossip[E](message: E)

case class GenericGossipMsg(mid: Array[Byte])

case class DeliverGossip[E](message: E)

case class Neighbors(neighbors: List[ActorRef])

object GetNeighbors





