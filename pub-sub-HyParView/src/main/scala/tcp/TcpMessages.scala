package tcp

/*import utils.Node

//TODO verificar que estas mensagens estao a ser recebidas ou enviadas no tcp client


case class TcpDisconnectOrBlocked(failedNode : Node)

case class TcpSuccess(remoteNode : Node)

case class TcpFailed(remoteNode : Node)

case class NeighborMsg(myNode : Node, priority : Int)

case class NeighborAccept(sender : Node)

case class NeighborReject(sender : Node)*/

case class TcpMessage[T](message : T)

case class WriteTcpMessage[T](tcpMessage: TcpMessage[T])