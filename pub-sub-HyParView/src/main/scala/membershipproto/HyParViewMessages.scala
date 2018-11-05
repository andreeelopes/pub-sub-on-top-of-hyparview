package membershipproto

import akka.actor.ActorRef

case class Join(newNode: ActorRef)

case class ForwardJoin(newNode: ActorRef, ttl: Long, sender: ActorRef)

case class Disconnect(peer: ActorRef)

case class Gossip(p: ActorRef, m: String, mid: String, myself: ActorRef)






