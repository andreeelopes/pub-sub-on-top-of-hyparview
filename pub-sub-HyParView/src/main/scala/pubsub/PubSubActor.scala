package pubsub

import java.util.Date

import akka.actor.{Actor, ActorLogging, ActorRef}
import gossip.{Gossip, GossipDelivery}
import utils.Utils

class PubSubActor(n: Int) extends Actor with ActorLogging {

  val diameter = math.log(n * 10).toInt
  var radiusSubsByTopic = Map[String, Set[(ActorRef, Date)]]()
  var mySubs = Map[String, Date]()
  val subHops = (diameter + 1) / 2
  val pubHops = (diameter + 1) / 2
  var delivered = Set[Array[Byte]]()

  val TTL = 30 //s

  var bcastActor: ActorRef = _
  var testAppActor: ActorRef = _


  //TODO  context.system.scheduler.schedule(interval = duration.Duration(Duration.Duration))


  override def receive = {

    case pubsub.Start(_broadcastActor_, _testAppActor_) =>

      log.info(s"Starting: diameter - $diameter ; subHops - $subHops")

      bcastActor = _broadcastActor_
      testAppActor = _testAppActor_

    case Subscribe(topic) => subscribe(topic)

    case Unsubscribe(topic) => unsubscribe(topic)

    case Publish(topic, m) => publish(topic, m)

    case GossipDelivery(message) => message match {

      case PassSubscribe(subscriber, topic, dateTTL, hops, mid) =>
        receivePassSub(PassSubscribe(subscriber, topic, dateTTL, hops, mid))

      case PassUnsubscribe(unsubscriber, topic, hops, mid) =>
        receivePassUnsub(PassUnsubscribe(unsubscriber, topic, hops, mid))

      case PassPublish(topic, hops, msg, mid) =>
        receivePassPub(PassPublish(topic, hops, msg, mid))

    }

    case DirectMessage(topic, message, mid) =>
      receiveDirectMsg(DirectMessage(topic, message, mid))

  }


  def subscribe(topic: String) = {
    log.info(s"Received subscribing $topic")

    val dateTTL = Utils.getDatePlusTime(TTL)
    val mid = Utils.md5("SUB" + topic + self + Utils.getDate)

    mySubs += (topic -> dateTTL)

    bcastActor ! Gossip(mid, PassSubscribe(self, topic, dateTTL, subHops - 1, mid))
  }

  def unsubscribe(topic: String) = {
    log.info(s"Received unsubscribing $topic")

    val mid = Utils.md5("UNSUB" + topic + self + Utils.getDate)

    mySubs -= topic

    bcastActor ! Gossip(mid, PassUnsubscribe(self, topic, subHops - 1, mid))
  }

  def publish(topic: String, m: String) = {
    log.info(s"Received publish ($topic) : $m")

    val mid = Utils.md5("PUB" + topic + self + m + Utils.getDate)

    bcastActor ! Gossip(mid, PassPublish(topic, pubHops - 1, m, mid))
  }


  def receivePassSub(passSubscribe: PassSubscribe) = {

    log.info(s"Received passPub: $passSubscribe")

    val setOpt = radiusSubsByTopic.get(passSubscribe.topic)

    radiusSubsByTopic =
      if (setOpt.isDefined) {
        val updatedSet = setOpt.get.filter(p => !p._1.equals(passSubscribe.subscriber)) + ((passSubscribe.subscriber, passSubscribe.dateTTL))

        radiusSubsByTopic.updated(passSubscribe.topic, updatedSet)
      } else {
        radiusSubsByTopic + (passSubscribe.topic -> Set((passSubscribe.subscriber, passSubscribe.dateTTL)))
      }

    if (passSubscribe.subHops > 0) {
      bcastActor ! Gossip(passSubscribe.mid, passSubscribe.copy(subHops = passSubscribe.subHops - 1))
    }

    log.info(s"radiusSubsByTopic : ${radiusSubsByTopic.toString()}")


  }

  def receivePassUnsub(passUnsubscribe: PassUnsubscribe) = {

    log.info(s"Received passUnsub: $passUnsubscribe")

    val updatedSet = radiusSubsByTopic(passUnsubscribe.topic).filter(p => !p._1.equals(passUnsubscribe.unsubscriber))

    radiusSubsByTopic = radiusSubsByTopic.updated(passUnsubscribe.topic, updatedSet)

    if (passUnsubscribe.unsubHops > 0) {
      bcastActor ! Gossip(passUnsubscribe.mid, passUnsubscribe.copy(unsubHops = passUnsubscribe.unsubHops - 1))
    }

    log.info(s"radiusSubsByTopic : ${radiusSubsByTopic.toString()}")


  }

  def receivePassPub(passPublish: PassPublish) = {

    log.info(s"Received PassPub: $passPublish")


    val dateTTLOpt = mySubs.get(passPublish.topic)
    if (dateTTLOpt.isDefined && dateTTLOpt.get.after(Utils.getDate))
      testAppActor ! PSDelivery(passPublish.topic, passPublish.message)


    radiusSubsByTopic(passPublish.topic)
      .filter(p => p._2.after(Utils.getDate))
      .foreach(p => p._1 ! DirectMessage(passPublish.topic, passPublish.message, passPublish.mid))

    if (passPublish.pubHops > 0) {
      bcastActor ! Gossip(passPublish.mid, passPublish.copy(pubHops = passPublish.pubHops - 1))
    }

    log.info(s"radiusSubsByTopic : ${radiusSubsByTopic.toString()}")

  }


  def receiveDirectMsg(directMessage: DirectMessage) = {

    if (!delivered.contains(directMessage.mid)) {

      log.info(s"Received DirectMsg: $directMessage")

      delivered += directMessage.mid

      val dateTTLOpt = mySubs.get(directMessage.topic)
      if (dateTTLOpt.isDefined && dateTTLOpt.get.after(Utils.getDate))
        testAppActor ! PSDelivery(directMessage.topic, directMessage.message)

    }

  }


  def renewSub() = {

    mySubs.filter(sub => sub._2.before(Utils.getDate)) //*0.2
      .foreach(sub => subscribe(sub._1))

    mySubs = mySubs.map(sub => (sub._1, Utils.getDatePlusTime(TTL)))
  }


  def cleanOldSubs() = {
    radiusSubsByTopic = radiusSubsByTopic.map(s => (s._1, s._2.filter(p => p._2.after(Utils.getDate))))
  }


}
