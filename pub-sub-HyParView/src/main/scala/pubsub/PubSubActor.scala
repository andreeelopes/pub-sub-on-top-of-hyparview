package pubsub

import java.util.concurrent.TimeUnit
import java.util.Date

import akka.actor.{Actor, ActorLogging}
import communication._
import membership.{MetricsDelivery, MetricsRequest}
import utils.{Node, Start, Utils}

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, FiniteDuration}

class PubSubActor(n: Int) extends Actor with ActorLogging {

  val diameter = math.log(n * 10).toInt
  var radiusSubsByTopic = Map[String, Set[(Node, Date)]]()
  var mySubs = Map[String, Date]()
  val subHops = (diameter + 1) / 2
  val pubHops = (diameter + 1) / 2

  val TTL = 30

  var myNode: Node = _


  def receiveDirectMsgDelivery(dm: DirectMessage): Unit = {
    val dateTTLOpt = mySubs.get(dm.topic)
    if (dateTTLOpt.isDefined && dateTTLOpt.get.after(Utils.getDate))
      myNode.testAppActor ! PSDelivery(dm.topic, dm.message)
  }

  override def receive = {

    case Start(node) =>

      log.info(s"Starting: diameter - $diameter ; subHops - $subHops")

      context.system.scheduler.schedule(FiniteDuration(10, TimeUnit.SECONDS),
        Duration(20, TimeUnit.SECONDS), self, RenewSubs)

      context.system.scheduler.schedule(FiniteDuration(10, TimeUnit.SECONDS),
        Duration(60, TimeUnit.SECONDS), self, CleanOldSubs)

      myNode = node

    //TestApp layer
    case Subscribe(topic) => subscribe(topic)

    //TestApp layer
    case Unsubscribe(topic) => unsubscribe(topic)

    //TestApp layer
    case Publish(topic, m) => publish(topic, m)

    //Gossip layer
    case GossipDelivery(message) => message match {

      case ps@PassSubscribe(_, _, _, _, _) =>
        receivePassSub(ps)

      case pu@PassUnsubscribe(_, _, _, _) =>
        receivePassUnsub(pu)

      case pp@PassPublish(_, _, _, _) =>
        receivePassPub(pp)

    }

    //PubSub layer
    case RenewSubs =>
      renewSub()

    //PubSub layer
    case CleanOldSubs =>
      cleanOldSubs()

    case d@DirectMessageDelivery(_) =>
      receiveDirectMsgDelivery(d.directMessage)

  }


  def subscribe(topic: String) = {
    log.info(s"Subscribing $topic")

    val dateTTL = Utils.getDatePlusTime(TTL)

    val mid = Utils.md5("SUB" + topic + myNode + System.currentTimeMillis()).toString

    mySubs += (topic -> dateTTL)

    myNode.communicationActor ! Gossip(mid, PassSubscribe(myNode, topic, dateTTL, subHops - 1, mid))
  }

  def unsubscribe(topic: String) = {
    log.info(s"Unsubscribing $topic")

    val mid = Utils.md5("UNSUB" + topic + myNode + System.currentTimeMillis()).toString

    mySubs -= topic

    myNode.communicationActor ! Gossip(mid, PassUnsubscribe(myNode, topic, subHops - 1, mid))
  }

  def publish(topic: String, m: String) = {
    log.info(s"Publishing ($topic) : $m")

    val mid = Utils.md5("PUB" + topic + myNode + System.currentTimeMillis()).toString

    if (mySubs.contains(topic))
      myNode.communicationActor ! DirectMessageRequest(myNode, DirectMessage(topic, m, mid)) //Deliver to me

    myNode.communicationActor ! Gossip(mid, PassPublish(topic, pubHops - 1, m, mid))
  }


  def receivePassSub(passSubscribe: PassSubscribe) = {

    log.info(s"Received passPub: $passSubscribe")

    val setOpt = radiusSubsByTopic.get(passSubscribe.topic)

    radiusSubsByTopic = if (setOpt.isDefined) {
      val updatedSet = setOpt.get.filter(p =>
        !p._1.equals(passSubscribe.subscriber)) + ((passSubscribe.subscriber, passSubscribe.dateTTL))

      radiusSubsByTopic.updated(passSubscribe.topic, updatedSet)
    } else {
      radiusSubsByTopic + (passSubscribe.topic -> Set((passSubscribe.subscriber, passSubscribe.dateTTL)))
    }

    if (passSubscribe.subHops > 0) {
      myNode.communicationActor ! Gossip(passSubscribe.mid.toString, passSubscribe.copy(subHops = passSubscribe.subHops - 1))
    }

    //    log.info(s"radiusSubsByTopic : ${radiusSubsByTopic.toString()}")


  }

  def receivePassUnsub(passUnsubscribe: PassUnsubscribe) = {

    log.info(s"Received passUnsub: $passUnsubscribe")

    val setOpt = radiusSubsByTopic.get(passUnsubscribe.topic)
    if (setOpt.isDefined) {
      val updatedSet = setOpt.get.filter(p => !p._1.equals(passUnsubscribe.unsubscriber))
      radiusSubsByTopic = radiusSubsByTopic.updated(passUnsubscribe.topic, updatedSet)
    }


    if (passUnsubscribe.unsubHops > 0) {
      myNode.communicationActor ! Gossip(passUnsubscribe.mid.toString, passUnsubscribe.copy(unsubHops = passUnsubscribe.unsubHops - 1))
    }

    log.info(s"radiusSubsByTopic : ${radiusSubsByTopic.toString()}")


  }

  def receivePassPub(passPublish: PassPublish) = {

    log.info(s"Received PassPub: $passPublish")


    val dateTTLOpt = mySubs.get(passPublish.topic)
    if (dateTTLOpt.isDefined && dateTTLOpt.get.after(Utils.getDate)) {
      myNode.testAppActor ! PSDelivery(passPublish.topic, passPublish.message)
    }

    val setOpt = radiusSubsByTopic.get(passPublish.topic)
    if (setOpt.isDefined) {
      setOpt.get.filter(p => p._2.after(Utils.getDate))
        .foreach { p =>
          myNode.communicationActor ! DirectMessageRequest(p._1, DirectMessage(passPublish.topic, passPublish.message, passPublish.mid))
        }
    }


    if (passPublish.pubHops > 0) {
      myNode.communicationActor ! Gossip(passPublish.mid.toString, passPublish.copy(pubHops = passPublish.pubHops - 1))
    }

    log.info(s"radiusSubsByTopic : ${radiusSubsByTopic.toString()}")

  }

  def renewSub() = {
    log.info("Trigger Renew subs")

    val subsToUpdate = mySubs.filter(sub => sub._2.before(Utils.getDatePlusTime((TTL * 0.2).toInt)))

    subsToUpdate.foreach(sub => subscribe(sub._1))

    mySubs = mySubs.map { sub =>
      if (subsToUpdate.get(sub._1).isDefined) (sub._1, Utils.getDatePlusTime(TTL))
      else sub
    }


  }

  def cleanOldSubs() = {
    log.info("Cleaning old subscriptions of other nodes")

    radiusSubsByTopic = radiusSubsByTopic.map(s => (s._1, s._2.filter(p => p._2.after(Utils.getDate))))
  }
}
