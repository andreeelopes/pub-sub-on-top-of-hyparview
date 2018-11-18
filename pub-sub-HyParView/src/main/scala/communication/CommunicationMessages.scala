package communication

import utils.Node


case class Gossip[A](mid: String, message: A)

case class GossipDelivery[A](message: A)

case class Send[A](mid: String, message: A)

case class PassGossip[A](mid: String, message: A)

case class DirectMessageRequest(target: Node, directMessage: DirectMessage)

case class DirectMessageDelivery(directMessage: DirectMessage)

case class DirectMessage(topic: String, message: String, mid: String)
