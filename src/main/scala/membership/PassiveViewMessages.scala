package membership

import utils.Node


object PassiveViewCyclicCheck

case class ShuffleMsg(senderNode: Node, exchangeList: List[Node], timeToLive: Int)

case class ShuffleReplyMsg(senderNode: Node, passiveViewSample: List[Node], receivedExchangeList: List[Node]) //exchange list should be removed to be more optimized



