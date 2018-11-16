package membership

import utils.Node

case class Join(newNode: Node)

case class ForwardJoin(newNode: Node, ttl: Long)

case class Disconnect(node: Node)

case class Neighbors(neighborsSample: List[Node])

case class GetNeighbors(n: Int)

case class Start(contactNode: Node, myNode: Node)

case class addToActiveWarning(senderNode : Node)

//Passive View management messages
case class PassiveViewCyclicCheck()

case class ShuffleMsg(senderNode : Node, exchangeList : List[Node], timeToLive :Int)

case class ShuffleReplyMsg(senderNode : Node, passiveViewSample : List[Node], receivedExchangeList : List[Node]) //exchange list should be removed to be more optimized

//Active View management messages
case class TcpDisconnectOrBlocked(failedNode : Node)

case class TcpSuccess(remoteNode : Node)

case class TcpFailed(remoteNode : Node)

case class Neighbor(senderNode : Node, priority: Int)

case class NeighborAccept(senderNode : Node)

case class NeighborReject(senderNode : Node)

case class AttemptTcpConnection(senderNode : Node)
