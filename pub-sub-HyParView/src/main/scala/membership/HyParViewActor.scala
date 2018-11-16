package membership

import akka.actor.{Actor, ActorIdentity, ActorLogging, Identify}
import utils.{Node, Utils}


class HyParViewActor extends Actor with ActorLogging {

  var activeView = List[Node]()
  var passiveView = List[Node]()
  val ARWL = 6
  val PRWL = 3
  val actViewMaxSize = 5 //TODO
  val passViewMaxSize = 30 //TODO

  var receivedMsgs = List[Array[Byte]]()


  var contactNode: Node = _
  var myNode: Node = _

  override def receive = {

    case s@Start(_, _) =>
      receiveStart(s)

    //membership layer
    case j@Join(_) =>
      receiveJoin(j)

    //membership layer
    case fj@ForwardJoin(_, _) =>
      receiveForwardJoin(fj)

    //membership layer
    case d@Disconnect(_) =>
      receiveDisconnect(d)

    //membership layer
    case a@ActorIdentity(_, _) =>
      receiveIdentifyReply(a)

    //membership layer
    case g@GetNode(_) =>
      g.sender.membershipActor ! myNode

    //membership layer
    case node@Node(_, _, _, _, _) =>
      contactNode = node
      log.info("Received contactNode: " + node)
      contactNode.membershipActor ! Join(myNode)

      addNodeActView(contactNode)

    //gossip layer
    case gn@GetNeighbors(_, _) =>
      receiveGetNeighbors(gn)

  }

  def receiveStart(startMsg: Start) = {

    myNode = startMsg.myNode

    if (startMsg.contactNodeId != null) {
      val selection = context.actorSelection(startMsg.contactNodeId)
      selection ! Identify()
    } else {
      log.warning("Contact node not provided")
    }

  }


  def receiveIdentifyReply(actorIdentity: ActorIdentity) = {
    actorIdentity.ref.get ! GetNode(myNode)
  }

  def receiveJoin(joinMsg: Join) = {
    log.info(s"Receiving: ${joinMsg.toString}")

    addNodeActView(joinMsg.newNode)
    activeView.filter(n => !n.equals(joinMsg.newNode))
      .foreach(n => n.membershipActor ! ForwardJoin(joinMsg.newNode, ARWL))
  }

  def receiveForwardJoin(forwardMsg: ForwardJoin) = {
    log.info(s"Receiving: ${forwardMsg.toString}")

    if (forwardMsg.ttl == 0 || activeView.size == 1)
      addNodeActView(forwardMsg.newNode)
    else {
      if (forwardMsg.ttl == PRWL)
        addNodePassView(forwardMsg.newNode)

      val n = Utils.pickRandomN[Node](activeView.filter(n => !n.equals(n)), 1).head

      n.membershipActor ! ForwardJoin(forwardMsg.newNode, forwardMsg.ttl - 1)
    }
  }

  def receiveDisconnect(disconnectMsg: Disconnect) = {
    if (activeView.contains(disconnectMsg.node)) {
      log.info(s"Receiving: ${disconnectMsg.toString}")

      activeView = activeView.filter(n => !n.equals(disconnectMsg.node))
      addNodePassView(disconnectMsg.node)
    }
  }

  def receiveGetNeighbors(getNeighborsMsg: GetNeighbors) = {
    val peersSample = getPeers(getNeighborsMsg.n, getNeighborsMsg.sender)

    log.info(s"Returning GetNeighbors: $peersSample")

    myNode.gossipActor ! Neighbors(peersSample)
  }

  def dropRandomElemActView() = {
    val n = Utils.pickRandomN[Node](activeView, 1).head
    n.membershipActor ! Disconnect(myNode)
    activeView = activeView.filter(elem => !elem.equals(n))
    passiveView ::= n

    log.info(s"Dropped $n from active view and added it to the passive")
  }

  def addNodeActView(newNode: Node) = {
    if (!newNode.equals(myNode) && !activeView.contains(newNode)) {
      if (activeView.size == actViewMaxSize)
        dropRandomElemActView()
      log.info(s"Adding $newNode to active view")
      activeView ::= newNode
      log.info(s" ActiveView: ${activeView.toString()} ; size: ${activeView.size}")
    }

  }

  def addNodePassView(newNode: Node) = {
    if (!newNode.equals(myNode) && !activeView.contains(newNode) && !passiveView.contains(newNode)) {
      if (passiveView.size == passViewMaxSize) {
        val n = Utils.pickRandomN[Node](activeView, 1).head
        passiveView = passiveView.filter(elem => !elem.equals(n))
      }
      log.info(s"Adding $newNode to passive view")
      log.info(s" Passive View: ${passiveView.toString()}")

      passiveView ::= newNode
    }

  }

  def getPeers(f: Int, sender: Node = null) = {
    log.info(s"Get peers to gossip")
    Utils.pickRandomN[Node](activeView, f, sender)
  }


}