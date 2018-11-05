package pubsub

import java.util.Date

import akka.actor.ActorRef

case class Subscribe(self: ActorRef, topic: String, dateTTL: Date, subHops: Int, mid: Array[Byte])

case class Unsubscribe(self: ActorRef, topic: String, unsubHops: Int, mid: Array[Byte])

case class Publish(self: ActorRef, topic: String, pubHops: Int, message: String, mid: Array[Byte])
