package membership

import akka.actor.ActorRef

case class Join(newNode: ActorRef)

case class ForwardJoin(newNode: ActorRef, ttl: Long)

object Disconnect

case class Gossip(m: String, mid: Array[Byte])

case class DeliverGossip(m: String)

case class Neighbors(neighbors: List[ActorRef]) //TODO

object GetNeighbors //TODO





