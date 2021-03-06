package pubsub

import java.util.Date

import utils.Node

case class Subscribe(topic: String)

case class Unsubscribe(topic: String)

case class Publish(topic: String, m: String)

case class PassSubscribe(subscriber: Node, topic: String, dateTTL: Date, subHops: Int, mid: String)

case class PassUnsubscribe(unsubscriber: Node, topic: String, unsubHops: Int, mid: String)

case class PassPublish(topic: String, pubHops: Int, message: String, mid: String)

case class PSDelivery(topic: String, m: String)

object RenewSubs

object CleanOldSubs