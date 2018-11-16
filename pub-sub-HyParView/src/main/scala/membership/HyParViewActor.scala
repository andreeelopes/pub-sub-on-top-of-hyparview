/*
package membership

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp.{CommandFailed, Connect}
import akka.util.ByteString
import tcp._
import utils.{Node, Utils}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, FiniteDuration}

//TODO TIRAR LENGTH POR SIZE EM TODO O LADO!
//TODO  verificar filters a ver se nao falta  bla = bla.filter(...)

class HyParViewActor extends Actor with ActorLogging {

  var activeView = Map[Node, ActorRef]()
  var pendingActiveView = Map[Node, ActorRef]() //necessary for implementation
  var passiveView = List[Node]()
  val ARWL = 6
  val PRWL = 3
  val actViewMaxSize = 5 //TODO
  val passViewMaxSize = 30 //TODO

  var receivedMsgs = List[Array[Byte]]()


  var contactNode: Node = _
  var myNode: Node = _

  //Active View Management variables
  val TcpUnsuccessful = 0
  val TcpSuccess = 1
  val HighPriority = 2
  val LowPriority = 3
  //Passive View Management variables
  val Ka = 1 //TODO
  val Kp = 1 //TODO
  val timeToLive = 3 //TODO
  context.system.scheduler.schedule(FiniteDuration(10, TimeUnit.SECONDS),
    Duration(20, TimeUnit.SECONDS), self, PassiveViewCyclicCheck)


  override def receive = {

    case s@Start(_, _, _) =>
      receiveStart(s)

    case j@Join(_) =>
      receiveJoin(j)

    case fj@ForwardJoin(_, _) =>
      receiveForwardJoin(fj)

    case d@Disconnect(_) =>
      receiveDisconnect(d)

    case GetNeighbors(n) =>
      receiveGetNeighbors(n)

    case TcpSuccess(node: Node) =>
      var priority = -1
      if (activeView.isEmpty)
        priority = HighPriority
      else
        priority = LowPriority
      var tcpClient = pendingActiveView(node)
      tcpClient ! NeighborMsg(myNode, priority) //TCPSend(q | NEIGHBOR, myself, priority)

    case TcpFailed(remoteNode: Node) =>
      dropNodePendingActView(remoteNode)

      dropNodeFromPassiveView(remoteNode)
      attemptActiveViewNodeReplacement(null)

    case CommandFailed(_: Connect) ⇒ //TODO

      //Active View Management
    case TcpDisconnectOrBlocked(failedNode : Node) =>
      dropNodeActiveView(failedNode)
      attemptActiveViewNodeReplacement(null)

    case Neighbor(node, priority) =>
      if (priority == HighPriority)
        addNodeActView(node)
      else {
        addNodePendingActView(node)
        var tcpClient = pendingActiveView(node)

        if (activeView.size != actViewMaxSize) {
          tcpClient ! NeighborAccept(myNode)
          dropNodePendingActView(node)
          addNodeActView(node)
        }
        else {
          tcpClient ! NeighborReject(myNode)
          dropNodePendingActView(node)
        }
      }

    case NeighborAccept(node: Node) =>
      dropNodeFromPassiveView(node)
      dropNodePendingActView(node)
      addNodeActView(node)

    case NeighborReject(node: Node) =>
      dropNodePendingActView(node)
      attemptActiveViewNodeReplacement(node)





    //Passive view management
    case PassiveViewCyclicCheck =>
      val q = Utils.pickRandomN(activeView.keys.toList, 1).head //sera da passive view ou da active view, o paper nao e explicito

      val exchangeList = myNode :: Utils.pickRandomN(passiveView, Kp) ::: Utils.pickRandomN(activeView.keys.toList, Ka)

      q.membershipActor ! ShuffleMsg(myNode, exchangeList, timeToLive)

    case ShuffleMsg(senderNode: Node, exchangeList: List[Node], timeToLive: Int) =>
      timeToLive -= 1
      if (timeToLive > 0 && activeView.size > 1) { // If TTL > 0 then keep random walk going
        val peer = Utils.pickRandomN(activeView.keys.toList.filter(node => !node.equals(senderNode)), 1).head
        peer.membershipActor ! ShuffleMsg(myNode, exchangeList, timeToLive)
      }
      else {
        val passiveViewSample = Utils.pickRandomN(passiveView, exchangeList.length)
        addNodePendingActView(senderNode)
        val tcpClient = pendingActiveView(senderNode)
        tcpClient ! TcpMessage(ShuffleReplyMsg(myNode, passiveViewSample, exchangeList))
        dropNodePendingActView(senderNode)
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
          passiveView = passiveView.filter(o => !o.equals(toRemove))

        }
        else {
          val randomToRemove = Utils.pickRandomN(passiveView, 1).head
          passiveView = passiveView.filter(p => !p.equals(randomToRemove))
        }
        addNodePassView(n)
      }
    }
  }

  def receiveStart(startMsg: Start) : Unit = {

    contactNode = startMsg.contactNode
    myNode = startMsg.myNode

    if (contactNode != null) {
      addNodeActView(contactNode)
      contactNode.membershipActor ! Join(myNode)
    }

  }

  def receiveJoin(joinMsg: Join) : Unit = {
    log.info(s"Receiving: ${joinMsg.toString}")

    addNodeActView(joinMsg.newNode)
    activeView.keys.filter(n => !n.equals(joinMsg.newNode))
      .foreach(n => n.membershipActor ! ForwardJoin(joinMsg.newNode, ARWL))
  }

  def receiveForwardJoin(forwardMsg: ForwardJoin) : Unit = {
    log.info(s"Receiving: ${forwardMsg.toString}")

    if (forwardMsg.ttl == 0 || activeView.size == 1)
      addNodeActView(forwardMsg.newNode)
    else {
      if (forwardMsg.ttl == PRWL)
        addNodePassView(forwardMsg.newNode)

      val n = Utils.pickRandomN[Node](activeView.keys.toList.filter(n => !n.equals(n)), 1).head

      n.membershipActor ! ForwardJoin(forwardMsg.newNode, forwardMsg.ttl - 1)
    }
  }

  def receiveDisconnect(disconnectMsg: Disconnect) : Unit = {
    if (activeView.contains(disconnectMsg.node)) {
      log.info(s"Receiving: ${disconnectMsg.toString}")

      activeView = activeView.filter(n => !n._1.equals(disconnectMsg.node))
      addNodePassView(disconnectMsg.node)
    }
  }

  def receiveGetNeighbors(n: Int) : Unit = {
    val peersSample = getPeers(n)

    log.info(s"Returning GetNeighbors: $peersSample")

    myNode.gossipActor ! Neighbors(peersSample)
  }

  def dropRandomElemActView() : Unit = {
    val n: Node = Utils.pickRandomN(activeView.keys.toList, 1).head
    n.membershipActor ! Disconnect(myNode)

    activeView -= n
    passiveView ::= n

    log.info(s"Dropped $n from active view and added it to the passive")
  }

  def addNodeActView(newNode: Node) : Unit = { //TODO - talvez seja melhor verificar se existe no pendingActView o new node e remover de la para nao existir duas ligacoes para o mesmo gajo
    if (!newNode.equals(myNode) && !activeView.contains(newNode)) {
      if (activeView.size == actViewMaxSize)
        dropRandomElemActView()
      log.info(s"Adding $newNode to active view")
      val tcpClient = context.system.actorOf(Props(new TcpClient(newNode.address, self, myNode)))
      activeView += (newNode -> tcpClient)
      log.info(s" ActiveView: ${activeView.toString()} ; size: ${activeView.size}")
    }

  }

  def addNodePassView(newNode: Node) : Unit = {
    if (!newNode.equals(myNode) && !activeView.contains(newNode) && !passiveView.contains(newNode)) {
      if (passiveView.size == passViewMaxSize) {
        val n: Node = Utils.pickRandomN(activeView.keys.toList, 1).head
        passiveView = passiveView.filter(elem => !elem.equals(n))
      }
      log.info(s"Adding $newNode to passive view")
      log.info(s" Passive View: ${passiveView.toString()}")

      passiveView ::= newNode
    }
  }

  def getPeers(f: Int): List[Node] = {
    log.info(s"Get peers to gossip")
    Utils.pickRandomN[Node](activeView.keys.toList, f)
  }

  def addNodePendingActView(remoteNode: Node): Unit = {
    val tcpClient = context.system.actorOf(Props(new TcpClient(remoteNode.address, self, myNode)))
    pendingActiveView += (remoteNode -> tcpClient)
  }

  def dropNodePendingActView(node: Node): Unit = {
    pendingActiveView -= node
  }

  def dropNodeFromPassiveView(q: Node) : Unit = {
    passiveView.filter(node => !node.equals(q))
  }

  def attemptActiveViewNodeReplacement(filterOut: Node) : Unit = {
    var q: Node = null

    if (filterOut == null) {
      q = Utils.pickRandomN(passiveView, 1).head
    }
    else {
      q = Utils.pickRandomN(passiveView.filter(node => !node.equals(filterOut)), 1).head
    }
    addNodePendingActView(q)
  }

  def dropNodeActiveView(failedNode: Node) : Unit = {
    activeView -= failedNode
  }

}
*/
