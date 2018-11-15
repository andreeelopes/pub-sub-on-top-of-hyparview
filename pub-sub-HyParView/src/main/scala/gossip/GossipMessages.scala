package gossip

import utils.Node


case class Gossip[A](mid: Array[Byte], message: A)

case class GossipDelivery[A](message: A)

case class Send[A](mid: Array[Byte], message: A)

case class PassGossip[A](mid: Array[Byte], message: A)
