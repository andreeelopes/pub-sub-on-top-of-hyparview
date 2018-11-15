package tcp

import utils.Node


case class TcpSuccess(remoteNode: Node)

case class NeighborMsg(myNode : Node, priority : Int)
