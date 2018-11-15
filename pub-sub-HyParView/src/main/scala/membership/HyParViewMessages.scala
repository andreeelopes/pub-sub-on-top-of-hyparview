package membership

import utils.Node

case class Join(newNode: Node)

case class ForwardJoin(newNode: Node, ttl: Long)

case class Disconnect(node: Node)

case class Neighbors(neighborsSample: List[Node])

case class GetNeighbors(n: Int)

case class Start(contactNode: Node, myNode: Node)





