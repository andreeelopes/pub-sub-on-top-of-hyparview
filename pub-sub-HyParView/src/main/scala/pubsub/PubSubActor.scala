package pubsub

import java.util.Date

import akka.actor.{Actor, ActorLogging, ActorRef}
import broadcast.Broadcast
import membership._
import utils.Utils

class PubSubActor(n: Int) extends Actor with ActorLogging {

  val diameter = math.log(n * 10)
  var radiusSubsByTopic = Map[String, Set[(ActorRef, Date)]]()
  var mySubs = Map[String, Date]()
  val subHops = ((diameter + 1) / 2).toInt
  val pubHops = ((diameter + 1) / 2).toInt
  var neighbors = List[ActorRef]()
  var delivered = Set[Array[Byte]]()
  var pendingSub = List[PassSubscribe]()
  var pendingUnsub = List[PassUnsubscribe]()
  var pendingPub = List[PassPublish]()
  val TTL = 30 //s

  var bcastActor: ActorRef = _
  var testAppActor: ActorRef = _


  //TODO  context.system.scheduler.schedule(interval = duration.Duration(Duration.Duration))


  override def receive = {

    case Start(_broadcastActor_, _testAppActor_) =>

      log.info(s"Starting: diameter - $diameter ; subHops - $subHops")

      bcastActor = _broadcastActor_
      testAppActor = _testAppActor_

    case Subscribe(topic) => subscribe(topic)

    case Unsubscribe(topic) => unsubscribe(topic)

    case Publish(topic, m) => publish(topic, m)

    case Broadcast(mid, message) => message match {

      case PassSubscribe(subscriber, topic, dateTTL, hops, mid) =>
        receivePassSub(PassSubscribe(subscriber, topic, dateTTL, hops, mid))

      case PassUnsubscribe(unsubscriber, topic, hops, mid) =>
        receivePassUnsub(PassUnsubscribe(unsubscriber, topic, hops, mid))

      case PassPublish(topic, hops, msg, mid) =>
        receivePassPub(PassPublish(topic, hops, msg, mid))

    }

    case DirectMessage(topic, message, mid) =>
      receiveDirectMsg(DirectMessage(topic, message, mid))

    case Neighbors(newNeighbors) =>
      receiveNeighbors(newNeighbors)
  }


  def subscribe(topic: String) = {
    log.info(s"Received subscribing $topic")

    val dateTTL = Utils.getDatePlusTime(TTL)
    val mid = Utils.md5("SUB" + topic + self + Utils.getDate)

    mySubs += (topic -> dateTTL)
    delivered += mid
    pendingSub ::= PassSubscribe(self, topic, dateTTL, subHops - 1, mid)

    bcastActor ! GetNeighbors
  }

  def unsubscribe(topic: String) = {
    log.info(s"Received unsubscribing $topic")

    val mid = Utils.md5("UNSUB" + topic + self + Utils.getDate)

    mySubs -= topic
    delivered += mid
    pendingUnsub ::= PassUnsubscribe(self, topic, subHops - 1, mid)
    bcastActor ! GetNeighbors
  }

  def publish(topic: String, m: String) = {
    log.info(s"Received publish ($topic) : $m")

    val mid = Utils.md5("PUB" + topic + self + m + Utils.getDate)

    delivered += mid
    pendingPub ::= PassPublish(topic, pubHops - 1, m, mid)
    bcastActor ! GetNeighbors
  }


  def receivePassSub(passSubscribe: PassSubscribe) = {
    if (!delivered.contains(passSubscribe.mid)) {

      log.info(s"Received passPub: $passSubscribe")

      val updatedSet = radiusSubsByTopic(passSubscribe.topic)
        .filter(p => !p._1.equals(passSubscribe.subscriber)) + ((passSubscribe.subscriber, passSubscribe.dateTTL))

      radiusSubsByTopic = radiusSubsByTopic.updated(passSubscribe.topic, updatedSet)


      if (passSubscribe.subHops > 0) {
        pendingSub ::= passSubscribe.copy(subHops = passSubscribe.subHops - 1)
        bcastActor ! GetNeighbors
      }

      log.info(s"radiusSubsByTopic : ${radiusSubsByTopic.toString()}")
    }


  }

  def receivePassUnsub(passUnsubscribe: PassUnsubscribe) = {
    if (!delivered.contains(passUnsubscribe.mid)) {

      log.info(s"Received passUnsub: $passUnsubscribe")


      val updatedSet = radiusSubsByTopic(passUnsubscribe.topic).filter(p => !p._1.equals(passUnsubscribe.unsubscriber))

      radiusSubsByTopic = radiusSubsByTopic.updated(passUnsubscribe.topic, updatedSet)

      delivered += passUnsubscribe.mid

      if (passUnsubscribe.unsubHops > 0) {
        pendingUnsub ::= passUnsubscribe.copy(unsubHops = passUnsubscribe.unsubHops - 1)
        bcastActor ! GetNeighbors
      }

      log.info(s"radiusSubsByTopic : ${radiusSubsByTopic.toString()}")

    }

  }

  def receivePassPub(passPublish: PassPublish) = {
    if (!delivered.contains(passPublish.mid)) {

      log.info(s"Received PassPub: $passPublish")


      delivered += passPublish.mid

      val dateTTLOpt = mySubs.get(passPublish.topic)
      if (dateTTLOpt.isDefined && dateTTLOpt.get.after(Utils.getDate))
        testAppActor ! PSDelivery(passPublish.topic, passPublish.message)


      radiusSubsByTopic(passPublish.topic)
        .filter(p => p._2.after(Utils.getDate))
        .foreach(p => p._1 ! DirectMessage(passPublish.topic, passPublish.message, passPublish.mid))

      if (passPublish.pubHops > 0) {
        pendingPub ::= passPublish.copy(pubHops = passPublish.pubHops - 1)
        bcastActor ! GetNeighbors
      }

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

  def receiveNeighbors(newNeighbors: List[ActorRef]) = {
    neighbors = newNeighbors

    log.info(s"pendingSub : $pendingPub")
    log.info(s"pendingUnsub : $pendingUnsub")
    log.info(s"pendingPub : $pendingPub")

    pendingSub.foreach { sub =>
      bcastActor ! Gossip(sub.mid, sub)
    }

    pendingUnsub.foreach { unsub =>
      bcastActor ! Gossip(unsub.mid, unsub)
    }

    pendingPub.foreach { pub =>
      bcastActor ! Gossip(pub.mid, pub)
    }

    pendingSub = List()
    pendingSub = List()
    pendingSub = List()

  }


  def renewSub() = {

    mySubs.filter(sub => sub._2.before(Utils.getDate)) //*0.2
      .foreach(sub => subscribe(sub._1))

    mySubs = mySubs.map(sub => (sub._1, Utils.getDatePlusTime(TTL)))
  }


  def cleanOldSubs() = {
    radiusSubsByTopic = radiusSubsByTopic // TODO remove based on value
  }


  //  def addToRadiusSubs(topic: String, process: ActorRef, ttl: Date) = {
  //    radiusSubsByProcess += (process -> (topic, ttl))
  //    radiusSubsByTopic += (topic -> (process, ttl))
  //  }
  //
  //  def removeFromRadiusSubs(topic: String, process: ActorRef) = {
  //    radiusSubsByTopic -= topic
  //    radiusSubsByProcess(process) = radiusSubsByProcess(process).filter(p => !p._1.equals(topic))
  //  }


}
