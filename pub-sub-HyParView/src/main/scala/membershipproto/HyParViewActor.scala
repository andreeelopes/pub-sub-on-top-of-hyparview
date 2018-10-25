package membershipproto

import akka.actor.{Actor, ActorRef}

class HyParViewActor extends Actor {


  def receive = {
    case Join(newNode) =>
    case ForwardJoin(newNode, ttl, sender) =>
    case Disconnect(peer) =>
    case Gossip(p, m, mid, myself) =>
  }

  def dropRandomElemActView = {

  }

  def addNodeActView(node: ActorRef) = {

  }

  def addNodePassView(node: ActorRef) = {

  }


  def broadcast(msg: String) = {

  }


}