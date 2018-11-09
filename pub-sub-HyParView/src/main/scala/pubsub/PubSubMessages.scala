package pubsub

import java.util.Date

import akka.actor.ActorRef

case class Subscribe(subscriber: ActorRef, topic: String, dateTTL: Date, subHops: Int, mid: Array[Byte])

case class Unsubscribe(unsubscriber: ActorRef, topic: String, unsubHops: Int, mid: Array[Byte])

case class Publish(topic: String, pubHops: Int, message: String, mid: Array[Byte])

case class DirectMessage(topic: String, message: String, mid: Array[Byte])