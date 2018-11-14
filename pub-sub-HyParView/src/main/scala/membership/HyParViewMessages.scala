package membership

import akka.actor.ActorRef

object Join

case class ForwardJoin(newNode: ActorRef, ttl: Long)

object Disconnect

case class Neighbors(neighborsSample: List[ActorRef])

case class GetNeighbors(n: Int)

case class Start(contactNode: ActorRef, bcastActor: ActorRef)





case class tcpDisconnectOrBlock()
case class Neighbor(priority: Int)

