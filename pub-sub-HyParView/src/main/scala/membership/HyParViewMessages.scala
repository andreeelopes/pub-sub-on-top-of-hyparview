package membership

import utils.Node


case class Join(newNode: Node)

case class ForwardJoin(newNode: Node, ttl: Long)

case class Disconnect(node: Node)

case class Neighbors(neighborsSample: List[Node])

case class GetNeighbors(n: Int, sender: Node = null)

case class Start(contactNodeId: String, myNode: Node)

case class StartLocal(contactNode : Node, myNode :Node)

case class addToActiveWarning(senderNode : Node)

case class IdentifyPartner(sender: Node)

case class GetNode(sender: Node)
