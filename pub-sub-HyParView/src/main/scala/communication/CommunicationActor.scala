package communication

import akka.actor.{Actor, ActorLogging}
import membership.{GetNeighbors, MetricsDelivery, MetricsRequest, Neighbors}
import pubsub.{PassPublish, Publish, Subscribe}
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
    case g@GossipRequest(_, _, _) =>
      receiveGossipRequest(g)

    //Gossip layer
    case s@Gossip(_, _) =>
      receiveGossip(s)

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


  def receiveGossipRequest[A](gossipMsg: GossipRequest[A]) = {

    log.info(s"Received Gossip Request: $gossipMsg")

    pending += (gossipMsg.mid -> gossipMsg)

    gossipMsg.message match {
      case PassPublish(_, _, _, _) =>
        val msg = gossipMsg.message.asInstanceOf[PassPublish]
        if (!delivered.contains(gossipMsg.mid)) {
          delivered += gossipMsg.mid
          myNode.pubSubActor ! DirectMessageDelivery(DirectMessage(msg.topic, msg.message, msg.mid))
        }
      case _ =>
    }

    myNode.membershipActor ! GetNeighbors(fanout, gossipMsg.senderNode)

  }


  def receiveGossip[A](sendMsg: Gossip[A]) = {

    if (!delivered.contains(sendMsg.mid)) {
      delivered += sendMsg.mid

      log.info(s"Received Gossip: $sendMsg")

      myNode.pubSubActor ! GossipDelivery(sendMsg.message)
    }
  }


  def receiveNeighbors[A](newNeighbors: List[Node]) = {
    neighbors = newNeighbors

    pending.foreach { msg =>
      neighbors.foreach { node =>
        log.info(s"Sending to $node gossip message: ${Gossip(msg._1, msg._2.asInstanceOf[GossipRequest[A]].message)}")

        node.communicationActor ! Gossip(msg._1, msg._2.asInstanceOf[GossipRequest[A]].message)
        outgoingMessages += 1
      }
    }

    pending = Map()
  }

  def receiveDirectMsg(directMessage: DirectMessage) = {
    if (!delivered.contains(directMessage.mid)) {
      delivered += directMessage.mid

      log.info(s"Received DirectMsg: $directMessage")


      myNode.pubSubActor ! DirectMessageDelivery(directMessage)
    }
  }

  def receiveDirectMsgRequest(req: DirectMessageRequest): Unit = {
    req.target.communicationActor ! req.directMessage
  }


}
