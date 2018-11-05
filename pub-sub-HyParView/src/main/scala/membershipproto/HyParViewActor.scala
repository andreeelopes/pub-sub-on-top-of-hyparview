package membershipproto

import akka.actor.{Actor, ActorRef}

import scala.util.Random


class HyParViewActor extends Actor {

  var activeView = List[ActorRef]()
  var passiveView = List[ActorRef]()
  var contactNode: ActorRef = null
  var newNode = null
  val ARWL = 6
  val PRWL = 3
  val actViewMaxSize = 7 //TODO

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

        val n = Utils.pickRandom[ActorRef](activeView.filter(n => !n.equals(sender)))

        n ! ForwardJoin(newNode, ttl - 1)
      }

    case Disconnect =>
      if (activeView.contains(sender))
        activeView = activeView.filter(n => !n.equals(sender))
      addNodePassView(sender)

    case Gossip(p, m, mid, myself) =>
  }

  def dropRandomElemActView = {
    val n = Utils.pickRandom[ActorRef](activeView)
    n ! Disconnect
    activeView = activeView.filter(elem => !elem.equals(n))
    passiveView = n :: passiveView
  }

  def addNodeActView(node: ActorRef) = {
    if (!node.equals(self) && !activeView.contains(node)) {
      if (activeView == actViewMaxSize)
        dropRandomElemActView
      activeView = node :: activeView
    }
    
  }

  def addNodePassView(node: ActorRef) = {

  }


  def broadcast(msg: String) = {
    //    val mID = Utils.md5(msg + this.toString)
    //    3 peerList
    //      getPeer(f,null)
    //


  }


}