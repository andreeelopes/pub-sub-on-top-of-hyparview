package gossip

import akka.actor.{Actor, ActorLogging, ActorRef}
import membership.{GetNeighbors, Neighbors}

class GossipActor(f: Int) extends Actor with ActorLogging {

  var neighbors = List[ActorRef]()
  var pending = Map[Array[Byte], Object]()
  var delivered = Set[Array[Byte]]()
  val fanout = f //TODO pass as a message of HyParView?

  var membershipActor: ActorRef = _
  var pubSubActor: ActorRef = _

  override def receive = {

    case s@Start(_membershipActor_, _pubSubActor_) =>
      receiveStart(s)

    case g@Gossip(mid, msg) =>
      receiveGossip(g, firstTime = true)

    case PassGossip(mid, msg) =>
      receiveGossip(Gossip(mid, msg))

    case Neighbors(neighborsSample) =>
      receiveNeighbors(neighborsSample)

    case s@Send(mid, msg) =>
      receiveSend(s)

  }


  def receiveStart(startMsg: Start) = {
    log.info("Starting")

    membershipActor = startMsg.membershipActor
    pubSubActor = startMsg.pubSubActor
  }

  def receiveGossip[A](gossipMsg: Gossip[A], firstTime: Boolean = false): Unit = {
    if (!delivered.contains(gossipMsg.mid)) {
      delivered += gossipMsg.mid


      if (!firstTime) {
        log.info(s"Delivered gossip message: $gossipMsg")
        pubSubActor ! GossipDelivery(gossipMsg.message)
      }

      pending += (gossipMsg.mid -> gossipMsg)

      membershipActor ! GetNeighbors(fanout)
    }

  }

  def receiveSend[A](sendMsg: Send[A]) = {
    if (!delivered.contains(sendMsg.mid)) {
      delivered += sendMsg.mid

      log.info(s"Delivered direct message: $sendMsg")

      pubSubActor ! GossipDelivery(sendMsg.message)
    }
  }


  def receiveNeighbors[A](newNeighbors: List[ActorRef]) = {
    neighbors = newNeighbors

    pending.foreach { msg =>
      neighbors.foreach { neighbor =>
        neighbor ! Send(msg._1, msg._2.asInstanceOf[Gossip[A]].message)
        //TODO neighbors são actores da camada de baixo e não desta
        //possível solução será manter sempre o par pubsub/membership
      }
    }

    pending = Map()
  }


}
