package pubsub

import java.util.Date

import akka.actor.ActorRef

case class Subscribe(topic: String)

case class Unsubscribe(topic: String)

case class Publish(topic: String, m: String)

case class PassSubscribe(subscriber: ActorRef, topic: String, dateTTL: Date, subHops: Int, mid: Array[Byte])

case class PassUnsubscribe(unsubscriber: ActorRef, topic: String, unsubHops: Int, mid: Array[Byte])

case class PassPublish(topic: String, pubHops: Int, message: String, mid: Array[Byte])

case class DirectMessage(topic: String, message: String, mid: Array[Byte])

case class PSDelivery(topic: String, m: String)

case class Start(bcastActor: ActorRef, testAppActor: ActorRef)