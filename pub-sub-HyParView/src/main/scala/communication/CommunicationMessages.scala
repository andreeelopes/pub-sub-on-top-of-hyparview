package communication

import utils.Node


case class Gossip[A](mid: Array[Byte], message: A)

case class GossipDelivery[A](message: A)

case class Send[A](mid: Array[Byte], message: A)

case class PassGossip[A](mid: Array[Byte], message: A)

case class DirectMessageRequest(target : Node, directMessage : DirectMessage)

case class DirectMessageDelivery(directMessage : DirectMessage)

case class DirectMessage(topic: String, message: String, mid: Array[Byte])
