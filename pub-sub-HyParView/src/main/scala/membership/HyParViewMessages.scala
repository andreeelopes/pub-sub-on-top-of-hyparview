package membership

import akka.actor.ActorRef

case class Join(newNode: ActorRef)

case class ForwardJoin(newNode: ActorRef, ttl: Long)

object Disconnect

case class Neighbors(neighbors: List[ActorRef])

object GetNeighbors

case class Start(contactNode: ActorRef, pubSubActor: ActorRef)





