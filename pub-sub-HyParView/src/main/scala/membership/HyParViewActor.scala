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
  val f = 3 //TODO


  var contactNode: ActorRef = _
  var pubSubActor: ActorRef = _

  override def receive = {

    case Start(_contactNode_, _pubSubActor_) =>
      log.info("Starting")

      contactNode = _contactNode_
      pubSubActor = _pubSubActor_

      if (contactNode != null) {
        addNodeActView(contactNode)
        contactNode ! Join
      }


    case Join =>
      log.info(s"Join from ${sender.path.name}")

      addNodeActView(sender)
      activeView.filter(n => !n.equals(sender)).foreach(n => n ! ForwardJoin(sender, ARWL))

    case ForwardJoin(newNode, ttl) =>
      log.info(s"ForwardJoin from ${sender.path.name} with message: ${ForwardJoin(newNode, ttl)}")

      if (ttl == 0 || activeView.size == 1)
        addNodeActView(newNode)
      else {
        if (ttl == PRWL)
          addNodePassView(newNode)

        val n = Utils.pickRandomN[ActorRef](activeView.filter(n => !n.equals(sender)), 1).head

        n ! ForwardJoin(newNode, ttl - 1)
      }

    case Disconnect =>
      if (activeView.contains(sender)) {
        log.info(s"Disconect ${sender.path.name}")

        activeView = activeView.filter(n => !n.equals(sender))
        addNodePassView(sender)
      }

    case Gossip(mid, message) =>
      if (!receivedMsgs.contains(mid)) {
        receivedMsgs ::= mid

        log.info(s"Delivered gossip message: $message")

        pubSubActor ! DeliverGossip(message)

        val peers = getPeers(f, sender)
        peers.foreach(p => p ! Gossip(mid, message))

      }


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

  def getPeers(f: Int, sender: ActorRef = null) = {
    log.info(s"Get peers to gossip")
    Utils.pickRandomN[ActorRef](activeView, f, sender)
  }


}