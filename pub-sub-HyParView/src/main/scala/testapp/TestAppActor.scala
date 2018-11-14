package testapp

import akka.actor.{Actor, ActorLogging, ActorRef}
import pubsub.{PSDelivery, Publish, Subscribe, Unsubscribe}


class TestAppActor extends Actor with ActorLogging {

  var pubSubActor: ActorRef = _

  override def receive = {
    case Start(_pubSubActor_) =>
      pubSubActor = _pubSubActor_
      log.info(s"Starting")

    case Subscribe(topic) =>
      log.info(s"Subscribing $topic")
      pubSubActor ! Subscribe(topic)
    case Unsubscribe(topic) =>
      log.info(s"Unsubscribing $topic")
      pubSubActor ! Unsubscribe(topic)

    case Publish(topic, m) =>
      log.info(s"Publishing ($topic): $m")
      pubSubActor ! Publish(topic, m)

    case PSDelivery(topic, m) =>
      log.info(s"Here's is something interesting for you about $topic:\n $m")
  }


}
