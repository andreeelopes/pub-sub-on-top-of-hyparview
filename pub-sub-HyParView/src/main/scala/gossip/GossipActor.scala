package gossip

import akka.actor.{Actor, ActorLogging, ActorRef}
import membership.{GetNeighbors, Neighbors}

class GossipActor(f: Int) extends Actor with ActorLogging {

  var neighbors = List[ActorRef]()
  var pending = List[Gossip]()
  var delivered = Set[Array[Byte]]()
  val fanout = f //TODO pass as a message of HyParView?

  var membershipActor: ActorRef = _
  var pubSubActor: ActorRef = _

  override def receive = {

    case Start(_membershipActor_, _pubSubActor_) =>
      receiveStart(Start(_membershipActor_, _pubSubActor_))

    case Gossip(mid, msg) =>
      receiveGossip(Gossip(mid, msg))

    case Neighbors(neighborsSample) =>
      receiveNeighbors(neighborsSample)

    case Send(mid, msg) =>
      receiveSend(Send(mid, msg))

  }


  def receiveStart(startMsg: Start) = {
    log.info("Starting")

    membershipActor = startMsg.membershipActor
    pubSubActor = startMsg.pubSubActor
  }

  def receiveGossip(gossipMsg: Gossip): Unit = {
    if (!delivered.contains(gossipMsg.mid)) {
      delivered += gossipMsg.mid

      log.info(s"Delivered gossip message: $gossipMsg")

      pubSubActor ! GossipDelivery(gossipMsg.message)

      pending ::= gossipMsg

      membershipActor ! GetNeighbors(fanout)
    }

  }

  def receiveSend(sendMsg: Send) = {
    if (!delivered.contains(sendMsg.mid)) {
      delivered += sendMsg.mid

      log.info(s"Delivered direct message: $sendMsg")

      pubSubActor ! GossipDelivery(sendMsg.message)
    }
  }


  def receiveNeighbors(newNeighbors: List[ActorRef]) = {
    neighbors = newNeighbors

    log.info(s"pending : $pending")

    pending.foreach { msg =>
      newNeighbors.foreach { neighbor =>
        neighbor ! Send(msg.mid, msg)
      }
    }

    pending = List()
  }


}
