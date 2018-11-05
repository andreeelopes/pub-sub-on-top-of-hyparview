package membershipproto

import akka.actor.ActorRef

case class Join(newNode: ActorRef)

case class ForwardJoin(newNode: ActorRef, ttl: Long)

object Disconnect

case class Gossip(p: ActorRef, m: String, mid: String, myself: ActorRef)






