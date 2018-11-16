package membership

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import utils.{Node, Utils}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, FiniteDuration}

//TODO TIRAR LENGTH POR SIZE EM TODO O LADO!
//TODO Verificar filters a ver se nao falta  bla = bla.filter(...)
//TODO Nao esquecer da simetria na active view


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

  //Active View Management variables
  val HighPriority = 1
  val LowPriority = 0
  //Passive View Management variables
  val Ka = 1 //TODO
  val Kp = 1 //TODO
  val timeToLive = 3 //TODO
  context.system.scheduler.schedule(FiniteDuration(10, TimeUnit.SECONDS),
    Duration(20, TimeUnit.SECONDS), self, PassiveViewCyclicCheck)


  override def receive = {

    case s@Start(_, _) =>
      receiveStart(s)

    case j@Join(_) =>
      receiveJoin(j)

    case fj@ForwardJoin(_, _) =>
      receiveForwardJoin(fj)

    case d@Disconnect(_) =>
      receiveDisconnect(d)

    case GetNeighbors(n) =>
      receiveGetNeighbors(n)

    //to achieve active view symmetry
    case addToActiveWarning(myNode) =>
      addNodeActView(myNode)

    case AttemptTcpConnection(senderNode) =>
      senderNode.membershipActor ! TcpSuccess(myNode)

    case TcpSuccess(senderNode: Node) =>
      var priority = -1
      if (activeView.isEmpty)
        priority = HighPriority
      else
        priority = LowPriority
      senderNode.membershipActor ! Neighbor(myNode, priority) //TCPSend(q | NEIGHBOR, myself, priority)

    case TcpFailed(remoteNode: Node) =>
      dropNodeFromPassiveView(remoteNode)
      attemptActiveViewNodeReplacement(null)

    //Active View Management
    case TcpDisconnectOrBlocked(failedNode: Node) =>
      dropNodeActiveView(failedNode)
      attemptActiveViewNodeReplacement(null)

    case Neighbor(senderNode, priority) =>
      if (priority == HighPriority) {
        addNodeActView(senderNode)
        senderNode.membershipActor ! NeighborAccept(myNode)
      }
      else {
        if (activeView.size != actViewMaxSize) {
          addNodeActView(senderNode)
          senderNode.membershipActor ! NeighborAccept(myNode)
        }
        else {
          senderNode.membershipActor ! NeighborReject(myNode)
        }
      }

    case NeighborAccept(node: Node) =>
      dropNodeFromPassiveView(node)
      addNodeActView(node)

    case NeighborReject(node: Node) =>
      attemptActiveViewNodeReplacement(node)


    //Passive view management
    case PassiveViewCyclicCheck =>
      val q = Utils.pickRandomN(passiveView, 1).head //TODO o artigo parece referir active enquanto o prof disse que era passive

      val exchangeList = myNode :: Utils.pickRandomN(passiveView, Kp) ::: Utils.pickRandomN(activeView, Ka)

      q.membershipActor ! ShuffleMsg(myNode, exchangeList, timeToLive)

    case ShuffleMsg(senderNode: Node, exchangeList: List[Node], ttl: Int) =>
      val newTtl = ttl - 1
      if (newTtl > 0 && activeView.size > 1) { // If TTL > 0 then keep random walk going
        val peer = Utils.pickRandomN(activeView.filter(node => !node.equals(senderNode)), 1).head
        peer.membershipActor ! ShuffleMsg(myNode, exchangeList, newTtl)
      }
      else {
        val passiveViewSample = Utils.pickRandomN(passiveView, exchangeList.length)
        sender ! ShuffleReplyMsg(myNode, passiveViewSample, exchangeList)
        mergePassiveView(exchangeList, passiveViewSample)
      }

    //note that here the note receiving the reply is the one who sent the exchangeList
    case ShuffleReplyMsg(_: Node, passiveViewSample: List[Node], exchangeList: List[Node]) =>
      mergePassiveView(passiveViewSample, exchangeList)
  }

  def mergePassiveView(toAdd: List[Node], sent: List[Node]): Unit = {
    val filteredToAddNodes = toAdd.filter(n => !passiveView.contains(n) && !n.equals(myNode))
    var sentPvNodes = sent.intersect(passiveView)
    filteredToAddNodes.foreach { n =>
      if (passiveView.size != passViewMaxSize)
        passiveView ::= n
      else {
        if (sentPvNodes.nonEmpty) {
          val toRemove = Utils.pickRandomN(sentPvNodes, 1).head
          sentPvNodes = sentPvNodes.filter(m => !m.equals(toRemove))
          dropNodeFromPassiveView(toRemove)
        }
        else {
          val randomToRemove = Utils.pickRandomN(passiveView, 1).head
          dropNodeFromPassiveView(randomToRemove)
        }
        addNodePassView(n)
      }
    }
  }

  def receiveStart(startMsg: Start): Unit = {
    contactNode = startMsg.contactNode
    myNode = startMsg.myNode

    if (contactNode != null) {
      addNodeActView(contactNode)
      contactNode.membershipActor ! Join(myNode)
    }
  }

  def receiveJoin(joinMsg: Join): Unit = {
    log.info(s"Receiving: ${joinMsg.toString}")

    addNodeActView(joinMsg.newNode)
    activeView.filter(n => !n.equals(joinMsg.newNode))
      .foreach(n => n.membershipActor ! ForwardJoin(joinMsg.newNode, ARWL))
  }

  def receiveForwardJoin(forwardMsg: ForwardJoin): Unit = {
    log.info(s"Receiving: ${forwardMsg.toString}")

    if (forwardMsg.ttl == 0 || activeView.size == 1)
      addNodeActView(forwardMsg.newNode, warnNewNode = true)
    else {
      if (forwardMsg.ttl == PRWL)
        addNodePassView(forwardMsg.newNode)

      val n = Utils.pickRandomN[Node](activeView.filter(n => !n.equals(n)), 1).head

      n.membershipActor ! ForwardJoin(forwardMsg.newNode, forwardMsg.ttl - 1)
    }
  }

  def receiveDisconnect(disconnectMsg: Disconnect): Unit = {
    if (activeView.contains(disconnectMsg.node)) {
      log.info(s"Receiving: ${disconnectMsg.toString}")

      activeView = activeView.filter(n => !n.equals(disconnectMsg.node))
      addNodePassView(disconnectMsg.node)
    }
  }

  def receiveGetNeighbors(n: Int): Unit = {
    val peersSample = getPeers(n)

    log.info(s"Returning GetNeighbors: $peersSample")

    myNode.gossipActor ! Neighbors(peersSample)
  }

  def dropRandomElemActView(): Unit = {
    val n: Node = Utils.pickRandomN(activeView, 1).head
    n.membershipActor ! Disconnect(myNode)

    dropNodeActiveView(n)
    addNodePassView(n)

    log.info(s"Dropped $n from active view and added it to the passive")
  }


  def getPeers(f: Int): List[Node] = {
    log.info(s"Get peers to gossip")
    Utils.pickRandomN[Node](activeView, f)
  }


  def attemptActiveViewNodeReplacement(filterOut: Node): Unit = {
    var q: Node = null

    if (filterOut == null) {
      q = Utils.pickRandomN(passiveView, 1).head
    }
    else {
      q = Utils.pickRandomN(passiveView.filter(node => !node.equals(filterOut)), 1).head
    }

    q.membershipActor ! AttemptTcpConnection(myNode)
  }

  def addNodeActView(newNode: Node, warnNewNode: Boolean = false): Unit = {
    if (!newNode.equals(myNode) && !activeView.contains(newNode)) {
      if (activeView.size == actViewMaxSize)
        dropRandomElemActView()
      log.info(s"Adding $newNode to active view")
      activeView ::= newNode
      if (warnNewNode)
        newNode.membershipActor ! addToActiveWarning(myNode)
      log.info(s" ActiveView: ${activeView.toString()} ; size: ${activeView.size}")
    }

  }

  def addNodePassView(newNode: Node): Unit = {
    if (!newNode.equals(myNode) && !activeView.contains(newNode) && !passiveView.contains(newNode)) {
      if (passiveView.size == passViewMaxSize) {
        val n: Node = Utils.pickRandomN(activeView, 1).head
        passiveView = passiveView.filter(elem => !elem.equals(n))
      }
      log.info(s"Adding $newNode to passive view")
      log.info(s" Passive View: ${passiveView.toString()}")

      passiveView ::= newNode
    }
  }

  def dropNodeActiveView(nodeToRemove: Node): Unit = {
    activeView = activeView.filter(n => !n.equals(nodeToRemove))
  }

  def dropNodeFromPassiveView(q: Node): Unit = {
    passiveView = passiveView.filter(node => !node.equals(q))
  }

}
