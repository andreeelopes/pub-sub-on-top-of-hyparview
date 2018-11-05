package broadcast

import akka.actor.ActorRef

case class Gossip(m: String, mid: Array[Byte])
