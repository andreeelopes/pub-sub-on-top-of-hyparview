package membership

import akka.actor.{Actor, ActorLogging, ActorRef}
import utils.Utils


class HyParViewActor extends Actor with ActorLogging {

  var activeView = List[ActorRef]()
  var passiveView = List[ActorRef]()
  val ARWL = 6
  val PRWL = 3
  val actViewMaxSize = 5 //TODO
  val passViewMaxSize = 30 //TODO

  var receivedMsgs = List[Array[Byte]]()


  var contactNode: ActorRef = _
  var bcastActor: ActorRef = _

  override def receive = {

    case s@Start(_contactNode_, _bcastActor_) =>
      receiveStart(s)

    case Join =>
      receiveJoin()

    case fj@ForwardJoin(newNode, ttl) =>
      receiveForwardJoin(fj)

    case Disconnect =>
      receiveDisconnect()

    case GetNeighbors(n) =>
      receiveGetNeighbors(n)

  }

  def receiveStart(startMsg: Start) = {
    log.info("Starting")

    contactNode = startMsg.contactNode
    bcastActor = startMsg.bcastActor

    if (contactNode != null) {
      addNodeActView(contactNode)
      contactNode ! Join
    }

  }

  def receiveJoin() = {
    log.info(s"Join from ${sender.path.name}")

    addNodeActView(sender)
    activeView.filter(n => !n.equals(sender)).foreach(n => n ! ForwardJoin(sender, ARWL))
  }

  def receiveForwardJoin(forwardMsg: ForwardJoin) = {
    log.info(s"ForwardJoin from ${sender.path.name} with message: $forwardMsg")

    if (forwardMsg.ttl == 0 || activeView.size == 1)
      addNodeActView(forwardMsg.newNode)
    else {
      if (forwardMsg.ttl == PRWL)
        addNodePassView(forwardMsg.newNode)

      val n = Utils.pickRandomN[ActorRef](activeView.filter(n => !n.equals(sender)), 1).head

      n ! ForwardJoin(forwardMsg.newNode, forwardMsg.ttl - 1)
    }
  }

  def receiveDisconnect() = {
    if (activeView.contains(sender)) {
      log.info(s"Disconect ${sender.path.name}")

      activeView = activeView.filter(n => !n.equals(sender))
      addNodePassView(sender)
    }
  }

  def receiveGetNeighbors(n: Int) = {
    val peersSample = getPeers(n)

    log.info(s"Returning GetNeighbors: $peersSample")

    bcastActor ! Neighbors(peersSample)
  }

  def dropRandomElemActView() = {
    val n = Utils.pickRandomN[ActorRef](activeView, 1).head
    n ! Disconnect
    activeView = activeView.filter(elem => !elem.equals(n))
    passiveView ::= n

    log.info(s"Dropped ${n.path.name} from active view and added it to the passive")
  }

  def addNodeActView(node: ActorRef) = {
    if (!node.equals(self) && !activeView.contains(node)) {
      if (activeView.size == actViewMaxSize)
        dropRandomElemActView()
      log.info(s"Adding ${node.path.name} to active view")
      activeView ::= node
      log.info(s" ActiveView: ${activeView.toString()} ; size: ${activeView.size}")
    }

  }

  def addNodePassView(node: ActorRef) = {
    if (!node.equals(self) && !activeView.contains(node) && !passiveView.contains(node)) {
      if (passiveView.size == passViewMaxSize) {
        val n = Utils.pickRandomN[ActorRef](activeView, 1).head
        passiveView = passiveView.filter(elem => !elem.equals(n))
      }
      log.info(s"Adding ${node.path.name} to passive view")
      log.info(s" Passive View: ${passiveView.toString()}")

      passiveView ::= node
    }

  }

  def getPeers(f: Int) = {
    log.info(s"Get peers to gossip")
    Utils.pickRandomN[ActorRef](activeView, f)
  }


}