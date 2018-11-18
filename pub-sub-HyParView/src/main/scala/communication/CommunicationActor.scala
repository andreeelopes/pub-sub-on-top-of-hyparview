package communication

import akka.actor.{Actor, ActorLogging}
import membership.{GetNeighbors, MetricsDelivery, MetricsRequest, Neighbors}
import utils.{Node, Start, Utils}

class CommunicationActor(f: Int) extends Actor with ActorLogging {

  var neighbors = List[Node]()
  var pending = Map[String, Object]()
  var delivered = Set[String]()
  val fanout = f

  var myNode: Node = _

  //Metrics variables
  var outgoingMessages = 0 //messages from this actor to some actor in another actor system (another node)
  var incomingMessages = 0 //messages received from another actor system (another node)


  override def receive = {

    case MetricsRequest =>
      myNode.testAppActor ! MetricsDelivery("communication", outgoingMessages, incomingMessages)

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

    //gossip layer
    case dm@DirectMessage(_, _, _) =>
      incomingMessages += 1
      receiveDirectMsg(dm)

    case req@DirectMessageRequest(_, _) =>
      receiveDirectMsgRequest(req)
  }


  def receiveStart(startMsg: Start) = {
    myNode = startMsg.node
  }


  def receivePassGossip[A](passGossipMsg: PassGossip[A]) = {
    incomingMessages += 1
    if (!delivered.contains(passGossipMsg.mid)) {

      log.info(s"Received PassGossip Request: $passGossipMsg")

      delivered += passGossipMsg.mid
      myNode.pubSubActor ! GossipDelivery(passGossipMsg.message)
    }
  }

  def receiveGossip[A](gossipMsg: Gossip[A]) = {
    if (!delivered.contains(gossipMsg.mid)) {
      delivered += gossipMsg.mid

      log.info(s"Received Gossip Request: $gossipMsg")

      pending += (gossipMsg.mid -> gossipMsg)
      myNode.membershipActor ! GetNeighbors(fanout, myNode)
      myNode
    }

  }

  def receiveSend[A](sendMsg: Send[A]) = {

    if (!delivered.contains(sendMsg.mid)) {
      delivered += sendMsg.mid

      log.info(s"Received Send: $sendMsg")

      myNode.pubSubActor ! GossipDelivery(sendMsg.message)
    }
  }


  def receiveNeighbors[A](newNeighbors: List[Node]) = {
    neighbors = newNeighbors

    pending.foreach { msg =>
      neighbors.foreach { node =>
        log.info(s"Sending to $node gossip message: ${Send(msg._1, msg._2.asInstanceOf[Gossip[A]].message)}")

        node.communicationActor ! Send(msg._1, msg._2.asInstanceOf[Gossip[A]].message)
        outgoingMessages += 1
      }
    }

    pending = Map()
  }

  def receiveDirectMsg(directMessage: DirectMessage) = {
    if (!delivered.contains(directMessage.mid)) {

      log.info(s"Received DirectMsg: $directMessage")

      delivered += directMessage.mid

      myNode.pubSubActor ! DirectMessageDelivery(directMessage)
    }
  }

  def receiveDirectMsgRequest(req: DirectMessageRequest): Unit = {
    req.target.communicationActor ! req.directMessage
  }


}
