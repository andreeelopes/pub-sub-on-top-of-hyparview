/*
package testapp

import akka.actor.{Actor, ActorLogging}
import pubsub.{PSDelivery, Publish, Subscribe, Unsubscribe}
import utils.{Node, Start}


class TestAppActor extends Actor with ActorLogging {

  var myNode: Node = _

  override def receive = {
    case Start(node) =>
      myNode = node

    case Subscribe(topic) =>
      log.info(s"Subscribing $topic")
      myNode.pubSubActor ! Subscribe(topic)
    case Unsubscribe(topic) =>
      log.info(s"Unsubscribing $topic")
      myNode.pubSubActor ! Unsubscribe(topic)

    case Publish(topic, m) =>
      log.info(s"Publishing ($topic): $m")
      myNode.pubSubActor ! Publish(topic, m)

    case PSDelivery(topic, m) =>
      log.info(s"Here's is something interesting for you about $topic:\n $m")
  }


}
*/
