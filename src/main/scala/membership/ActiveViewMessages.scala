package membership

import utils.Node


case class TcpDisconnectOrBlocked(failedNode : Node)

case class TcpSuccess(remoteNode : Node)

case class TcpFailed(remoteNode : Node)

case class Neighbor(senderNode : Node, priority: Int)

case class NeighborAccept(senderNode : Node)

case class NeighborReject(senderNode : Node)

case class AttemptTcpConnection(senderNode : Node)