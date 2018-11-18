package gossip

import akka.actor.{Actor, ActorLogging}
import membership.{GetNeighbors, MetricsDelivery, MetricsRequest, Neighbors}
import utils.{Node, Start}

class GossipActor(f: Int) extends Actor with ActorLogging {

  var neighbors = List[Node]()
  var pending = Map[Array[Byte], Object]()
  var delivered = Set[Array[Byte]]()
  val fanout = f //TODO pass as a message of HyParView?

  var myNode: Node = _

  //Metrics variables
  var outgoingMessages = 0 //messages from this actor to some actor in another actor system (another node)
  var incomingMessages = 0 //messages received from another actor system (another node)

  override def receive = {

    case MetricsRequest => //TODO double-check
      myNode.testAppActor ! MetricsDelivery("gossip",outgoingMessages, incomingMessages)

    case s@Start(_) =>
      receiveStart(s)

    //Pubsub layer
    case g@Gossip(_, _) =>
      receiveGossip(g)

    //Gossip layer
    case pg@PassGossip(_, _) =>
      receivePassGossip(pg)

    //Gossip layer
    case s@Send(_, _) =>
      receiveSend(s)

    //Membership layer
    case Neighbors(nodes) =>
      receiveNeighbors(nodes)
  }


  def receiveStart(startMsg: Start) = {
    myNode = startMsg.node
  }


  def receivePassGossip(passGossipMsg: PassGossip[Any]) = {
    incomingMessages += 1
    if (!delivered.contains(passGossipMsg.mid)) {
      delivered += passGossipMsg.mid
      myNode.pubSubActor ! GossipDelivery(passGossipMsg.message)
    }
  }

  def receiveGossip[A](gossipMsg: Gossip[A]) = {
    if (!delivered.contains(gossipMsg.mid)) {
      delivered += gossipMsg.mid

      pending += (gossipMsg.mid -> gossipMsg)
      myNode.membershipActor ! GetNeighbors(fanout, myNode)
    }

  }

  def receiveSend[A](sendMsg: Send[A]) = {
    if (!delivered.contains(sendMsg.mid)) {
      delivered += sendMsg.mid

      myNode.pubSubActor ! GossipDelivery(sendMsg.message)
    }
  }


  def receiveNeighbors[A](newNeighbors: List[Node]) = {
    neighbors = newNeighbors

    pending.foreach { msg =>
      neighbors.foreach { node =>
        log.info(s"Sending to $node gossip message: ${Send(msg._1, msg._2.asInstanceOf[Gossip[A]].message)}")

        node.gossipActor ! Send(msg._1, msg._2.asInstanceOf[Gossip[A]].message)
        outgoingMessages += 1
      }
    }

    pending = Map()
  }


}
