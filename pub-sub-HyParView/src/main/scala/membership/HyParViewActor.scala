package membership

import akka.actor.{Actor, ActorRef}


class HyParViewActor(implicit n: Int, contactNode: ActorRef, pubSubActor: ActorRef) extends Actor {

  var activeView = List[ActorRef]()
  var passiveView = List[ActorRef]()
  val ARWL = 6
  val PRWL = 3
  val actViewMaxSize = 5 //TODO
  val passViewMaxSize = 30 //TODO

  var receivedMsgs = List[Array[Byte]]()
  val f = 3 //TODO

  addNodeActView(contactNode)
  contactNode ! Join


  def receive = {
    case Join =>
      addNodeActView(sender)
      activeView.filter(n => n.equals(sender)).foreach(n => n ! ForwardJoin(sender, ARWL))

    case ForwardJoin(newNode, ttl) =>
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
        activeView = activeView.filter(n => !n.equals(sender))
        addNodePassView(sender)
      }

    case Gossip(m, mid) =>
      if (!receivedMsgs.contains(mid)) {
        receivedMsgs ::= mid

        pubSubActor ! DeliverGossip(m)

        val peers = getPeers(f, sender)
        peers.foreach(p => p ! Gossip(m, mid))

      }


  }

  def dropRandomElemActView() = {
    val n = Utils.pickRandomN[ActorRef](activeView, 1).head
    n ! Disconnect
    activeView = activeView.filter(elem => !elem.equals(n))
    passiveView ::= n
  }

  def addNodeActView(node: ActorRef) = {
    if (!node.equals(self) && !activeView.contains(node)) {
      if (activeView.size == actViewMaxSize)
        dropRandomElemActView()
      activeView ::= activeView
    }

  }

  def addNodePassView(node: ActorRef) = {
    if (!node.equals(self) && !activeView.contains(node) && !passiveView.contains(node)) {
      if (passiveView.size == passViewMaxSize) {
        val n = Utils.pickRandomN[ActorRef](activeView, 1).head
        passiveView = passiveView.filter(elem => !elem.equals(n))
      }
      passiveView ::= passiveView
    }

  }

  def getPeers(f: Int, sender: ActorRef = null) = {
    Utils.pickRandomN[ActorRef](activeView, f, sender)
  }


}