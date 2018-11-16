package membership

import java.util.Date
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorIdentity, ActorLogging, Cancellable, Identify}
import utils.{Node, Utils}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, FiniteDuration}

//TODO Verificar filters a ver se nao falta  bla = bla.filter(...)
//TODO Nao esquecer da simetria na active view


class HyParViewActor extends Actor with ActorLogging {

  var activeView: Map[Node, Date] = Map[Node, Date]()
  var passiveView: List[Node] = List[Node]()
  val ARWL = 3
  val PRWL = 3
  val actViewMaxSize = 4
  val passViewMaxSize = 10

  var receivedMsgs: List[Array[Byte]] = List[Array[Byte]]()

  var contactNode: Node = _
  var myNode: Node = _

  //Active View Management variables
  val HighPriority = 1
  val LowPriority = 0
  //Passive View Management variables
  val Ka = 2
  val Kp = 2
  val timeToLive = 2 //TTL for the exchange list TODO

  var tcpAttempts: Map[Node, Cancellable] = Map[Node, Cancellable]()
  val HeartBeatPeriod = 2 //in seconds

  context.system.scheduler.schedule(FiniteDuration(1, TimeUnit.SECONDS),
    Duration(30, TimeUnit.SECONDS), self, PassiveViewCyclicCheck)

  //context.system.scheduler.schedule(FiniteDuration(1, TimeUnit.SECONDS),
  //Duration(2, TimeUnit.SECONDS), self, ActiveViewCyclicCheck)


  override def receive = {

    case s@Start(_, _) =>
      receiveStart(s)

    case StartLocal(_contactNode_ : Node, _myNode_ : Node) =>
      contactNode = _contactNode_
      myNode = _myNode_
      //log.info("Received contactNode: " + contactNode)
      contactNode.membershipActor ! Join(myNode)

      addNodeActView(contactNode)

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

    //to achieve active view symmetry
    case addToActiveWarning(senderNode) =>
      addNodeActView(senderNode)

    case AttemptTcpConnection(senderNode) =>
      senderNode.membershipActor ! TcpSuccess(myNode)

    case TcpSuccess(senderNode: Node) =>
      var priority = -1
      if (activeView.isEmpty)
        priority = HighPriority
      else
        priority = LowPriority
      log.info(s"TCP: connection success with $senderNode sending a NeighborRequest: ${Neighbor(myNode, priority)}")
      senderNode.membershipActor ! Neighbor(myNode, priority) //TCPSend(q | NEIGHBOR, myself, priority)

    case TcpFailed(remoteNode: Node) =>
      log.info(s"TCP: connection failed with $remoteNode")
      dropNodeFromPassiveView(remoteNode)
      attemptActiveViewNodeReplacement(null)

    case Neighbor(senderNode, priority) =>
      log.info(s"Active Management: Got a Neighbor Request from $senderNode with priority $priority!\n\t | #ActiveView = ${activeView.size} of $actViewMaxSize")
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
      log.info(s"Got a Neighbor Accept from $node")
      dropNodeFromPassiveView(node)
      addNodeActView(node)

    case NeighborReject(node: Node) =>
      log.info(s"Got a Neighbor Reject from $node")
      attemptActiveViewNodeReplacement(node)

    case ActiveViewCyclicCheck() =>
      log.info(s"Active View Periodic Check aka heartbeat")
      val aliveNodes = activeView.filter(pair => pair._2.after(Utils.getDatePlusTime(-(3 * HeartBeatPeriod))))
      val deadNodes = activeView.filter(pair => !aliveNodes.contains(pair._1))

      deadNodes.foreach(dead => myNode.membershipActor ! TcpDisconnectOrBlocked(dead._1))
      aliveNodes.foreach(p => p._1.membershipActor ! Heartbeat)

    //Passive view management
    case PassiveViewCyclicCheck =>
      val q = Utils.pickRandomN(activeView.keys.toList, 1).head
      log.info(s"Passive View Periodic Check | target of Shuffle Message = $q")

      val exchangeList = myNode :: Utils.pickRandomN(passiveView, Kp) ::: Utils.pickRandomN(activeView.keys.toList, Ka)

      q.membershipActor ! ShuffleMsg(myNode, exchangeList, timeToLive)

    case ShuffleMsg(senderNode: Node, exchangeList: List[Node], ttl: Int) =>
      val newTtl = ttl - 1
      if (newTtl > 0 && activeView.size > 1) { // If TTL > 0 then keep random walk going
        val peer = Utils.pickRandomN(activeView.keys.toList.filter(node => !node.equals(senderNode)), 1).head
        log.info(s"Shuffle: Forwarded to $peer")
        Thread.sleep(5000) //TODO remove
        peer.membershipActor ! ShuffleMsg(myNode, exchangeList, newTtl)
      }
      else {
        val passiveViewSample = Utils.pickRandomN(passiveView, exchangeList.size)
        log.info(s"Shuffle: Replied to $senderNode")
        Thread.sleep(5000) //TODO remove
        senderNode.membershipActor ! ShuffleReplyMsg(myNode, passiveViewSample, exchangeList)

        mergePassiveView(exchangeList, passiveViewSample)
      }

    //note that here the note receiving the reply is the one who sent the exchangeList
    case ShuffleReplyMsg(senderNode: Node, passiveViewSample: List[Node], exchangeList: List[Node]) =>
      log.info(s"Shuffle: Got Shuffle Reply from $senderNode")
      Thread.sleep(5000) //TODO remove

      mergePassiveView(passiveViewSample, exchangeList)
  }

  def TcpDisconnectOrBlocked(failedNode: Node) = {
    log.info(s"TCP: connection with $failedNode might have failed")
    dropNodeActiveView(failedNode)
    attemptActiveViewNodeReplacement(null)
  }

  def mergePassiveView(toAdd: List[Node], sent: List[Node]): Unit = {
    log.info(s"Merging $toAdd into Passive | sent: $sent")
    Thread.sleep(5000)
    //TODO remove
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

    printPassiveViewState()
    printActiveViewState()
  }

  def receiveStart(startMsg: Start): Unit = {
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
    //log.info(s"Join: ${joinMsg.newNode} wants to join")

    addNodeActView(joinMsg.newNode)
    activeView.keys.toList.filter(n => !n.equals(joinMsg.newNode))
      .foreach(n => n.membershipActor ! ForwardJoin(joinMsg.newNode, ARWL))
  }

  def receiveForwardJoin(forwardMsg: ForwardJoin): Unit = {
    //log.info(s"Receiving: ${forwardMsg.toString}")

    if (forwardMsg.ttl == 0 || activeView.size == 1) {
      //log.info(s"Forward Join: Stopped on me | Gonna Add Active")
      addNodeActView(forwardMsg.newNode, warnNewNode = true)
    }
    else {
      if (forwardMsg.ttl == PRWL) {
        addNodePassView(forwardMsg.newNode)
        //log.info(s"Forward Join: Stopped on me | Gonna Add Passive")
      }

      val n = Utils.pickRandomN[Node](activeView.keys.toList.filter(n => !n.equals(n)), 1).head

      //log.info(s"Forward Join: forwarding to $n")
      n.membershipActor ! ForwardJoin(forwardMsg.newNode, forwardMsg.ttl - 1)
    }
  }

  def receiveDisconnect(disconnectMsg: Disconnect): Unit = {
    if (activeView.contains(disconnectMsg.node)) {
      log.info(s"Disconnect from: ${disconnectMsg.node}")

      activeView = activeView.filter(n => !n._1.equals(disconnectMsg.node))
      addNodePassView(disconnectMsg.node)
    }
  }

  def receiveGetNeighbors(getNeighborsMsg: GetNeighbors): Unit = {
    val peersSample = getPeers(getNeighborsMsg.n, getNeighborsMsg.sender)

    log.info(s"Returning GetNeighbors: $peersSample")

    myNode.gossipActor ! Neighbors(peersSample)
  }

  def attemptActiveViewNodeReplacement(filterOut: Node): Unit = {
    log.info(s"Management Active View: attempting")

    var q: Node = null

    if (filterOut == null) {
      q = Utils.pickRandomN(passiveView, 1).head
    }
    else {
      q = Utils.pickRandomN(passiveView.filter(node => !node.equals(filterOut)), 1).head
    }

    q.membershipActor ! AttemptTcpConnection(myNode)
    val timer = context.system.scheduler.schedule(Duration.Zero,
      Duration(50 * HeartBeatPeriod, TimeUnit.SECONDS), self, TcpFailed(q)) //TODO POR O TIMER A 3* this timer is used to fake a Tcp connection failure
    tcpAttempts += (q -> timer)
  }

  def addNodeActView(newNode: Node, warnNewNode: Boolean = false): Unit = {
    if (!newNode.equals(myNode) && !activeView.contains(newNode)) {
      if (activeView.size == actViewMaxSize)
        dropRandomElemActView()
      log.info(s"Active View: Add($newNode)")
      activeView += (newNode -> Utils.getDate)
      if (warnNewNode)
        newNode.membershipActor ! addToActiveWarning(myNode)
      log.info(s"Active View: ${activeView.map(p => p._1)} \n\t | size: ${activeView.size}")
    }

  }

  def addNodePassView(newNode: Node): Unit = {
    if (!newNode.equals(myNode) && !activeView.contains(newNode) && !passiveView.contains(newNode)) {
      if (passiveView.size == passViewMaxSize) {
        val n: Node = Utils.pickRandomN(activeView.keys.toList, 1).head
        passiveView = passiveView.filter(elem => !elem.equals(n))
      }
      log.info(s"Passive View: Add($newNode)")
      log.info(s"Passive View: ${passiveView.toString()} \n\t | size: ${passiveView.size}")

      passiveView ::= newNode
    }
  }

  def dropNodeActiveView(nodeToRemove: Node): Unit = {
    activeView = activeView.filter(n => !n._1.equals(nodeToRemove))
    log.info(s"Active View: Drop($nodeToRemove)")
    log.info(s"Active View: ${activeView.map(p => p._1)} \n\t | size: ${activeView.size}")

  }

  def getPeers(f: Int, sender: Node = null): List[Node] = {
    log.info(s"Got peers to gossip")
    Utils.pickRandomN[Node](activeView.keys.toList, f, sender)
  }


  def dropNodeFromPassiveView(q: Node): Unit = {
    log.info(s"Passive View: Drop($q)")
    log.info(s"Passive View: ${passiveView.toString()} \n\t | size: ${passiveView.size}")

    passiveView = passiveView.filter(node => !node.equals(q))
  }

  def dropRandomElemActView(): Unit = {

    val n: Node = Utils.pickRandomN(activeView.keys.toList, 1).head
    n.membershipActor ! Disconnect(myNode)

    log.info(s"Dropped $n from active view and added it to the passive")

    dropNodeActiveView(n)
    addNodePassView(n)
  }


  def printPassiveViewState(): Unit = {
    log.info(s"Passive View: ${passiveView.toString()} \n\t | size: ${passiveView.size}")
  }

  def printActiveViewState(): Unit = {
    log.info(s"Active View: ${activeView.map(p => p._1)} \n\t | size: ${activeView.size}")
  }
}
