package membership


import akka.actor.{Actor, ActorLogging, ActorRef, Props}
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

  //Active View Management variables
  val TcpUnsuccessful = 0
  val TcpSuccess = 1
  val HighPriority = 2
  val LowPriority = 3
  //Passive View Management variables
  val Ka = 1 //TODO
  val Kp = 1 //TODO


  //TODO nelson gestao da active view
  def dropNodeFromPassiveView(q: ActorRef) = {
    passiveView.filter(node => !node.equals(q))
  }

  //TODO nelson gestao da active view
  def attemptActiveViewNodeReplacement(filterOut: ActorRef): Boolean = {
    var q: ActorRef = null

    if (passiveView.isEmpty)
      return false

    if (filterOut == null) {
      q = Utils.pickRandomN(passiveView, 1).head
    }
    else {
      q = Utils.pickRandomN(passiveView.filter(node => !node.equals(sender)), 1).head
    }


    var status = TcpSuccess
    //TODO tentar estabelecer conexao
    var priority = -1

    if (status == TcpUnsuccessful) {
      dropNodeFromPassiveView(q)
      attemptActiveViewNodeReplacement(null) //TODO recursao?
    }
    else {
      if (activeView.isEmpty)
        priority = HighPriority
      else
        priority = LowPriority
      //TCPSend(q | NEIGHBOR, myself, priority) TODO
      return true
    }
  }

  override def receive = {

    case Start(_contactNode_, _bcastActor_) =>
      receiveStart(Start(_contactNode_, _bcastActor_))

    case Join =>
      receiveJoin()

    case ForwardJoin(newNode, ttl) =>
      receiveForwardJoin(ForwardJoin(newNode, ttl))

    case Disconnect =>
      receiveDisconnect()

    case GetNeighbors(n) =>
      receiveGetNeighbors(n)


    //TODO nelson gestao da active view
    case tcpDisconnectOrBlocking =>
      attemptActiveViewNodeReplacement(null)


    case Neighbor(priority) =>
      if (priority == HighPriority)
        addNodeActView(sender)
      else {
        if (activeView.length != actViewMaxSize) {
          addNodeActView(sender)
          //send pela conexao tcp ja aberta NeigborAccept
        }
        else
        //send pela conexao tcp ja aberta NeighborReject
      }

    case NeighborAccept =>
      dropNodeFromPassiveView(sender)
      addNodeActView(sender)

    case NeighborReject =>
      attemptActiveViewNodeReplacement(sender)

    //TODO nelson gestao da passive view
    case PassiveViewCyclicCheck =>
      val q: ActorRef = Utils.pickRandomN(passiveView, 1).head
      //TODO sera da passive view ou da active view, o paper nao e explicito
      val exchangeList = List(self,
        Utils.pickRandomN(passiveView, Kp),
        Utils.pickRandomN(activeView, Ka))
    //TCP(q | SHUFFLE, exchangeList, timeToLive)

    case Shuffle(exchangeList: List[ActorRef], timeToLive: Int) =>
      timeToLive -= 1
      if (timeToLive > 0 && activeView.length > 1) {
        val peer = Utils.pickRandomN(active.filter(node => !node.equals(sender)), 1).head
        //TODO - random walk nao e uma chamada ao TCP?
        TCP(peer | Shuffle, exchangeList, timeToLive)
      }
      else {
        var passiveViewSample = Utils.pickRandomN(passiveView, exchangeList.length)
        //SenderPorTCPTemporario(sender | ShuffleReply, passiveViewSample)
        mergePassiveView(exchangeList) //TODO
        // adicionar a exchange list a passive view (filtrar o myself e elementos que já estejam na pv)
        // se pv estiver cheia tirar nós para por estes novos, primeiro tenta-se tirar os nós que foram enviados para o outro peer se mesmo assim não der tira-se aleatoriamente

      }

    case ShuffleReply(passiveViewSample: List[ActorRef]) =>
      mergePassiveView(exchangeList) //TODO


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
