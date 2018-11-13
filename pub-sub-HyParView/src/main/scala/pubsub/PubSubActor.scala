package pubsub

import java.util.Date

import akka.actor.{Actor, ActorRef}
import membership.{GetNeighbors, Gossip, Neighbors}
import utils.Utils

class PubSubActor(n: Int, membershipActor: ActorRef, testAppActor: ActorRef) extends Actor {

  val diameter = math.log(n * 10)
  var radiusSubsByTopic = Map[String, Set[(ActorRef, Date)]]()
  var mySubs = Map[String, Date]() // poderão as minhas subs irem parar ao radiusSubsByTopic?
  val subHops = ((diameter + 1) / 2).toInt
  val pubHops = ((diameter + 1) / 2).toInt
  var neighbors = List[ActorRef]()
  var delivered = Set[Array[Byte]]()
  var pendingSub = List[Subscribe]()
  var pendingUnsub = List[Unsubscribe]()
  var pendingPub = List[Publish]()
  val TTL = 30 //s

  //  context.system.scheduler.schedule(interval = duration.Duration(Duration.Duration))


  def subscribe(topic: String) = {
    val dateTTL = Utils.getDatePlusTime(TTL)
    val mid = Utils.md5("SUB" + topic + self + Utils.getDate)

    mySubs += (topic -> dateTTL)
    delivered += mid
    pendingSub ::= Subscribe(self, topic, dateTTL, subHops - 1, mid)

    membershipActor ! GetNeighbors
  }

  def unsubscribe(topic: String) = {
    val mid = Utils.md5("UNSUB" + topic + self + Utils.getDate)

    mySubs -= topic
    delivered += mid
    pendingUnsub ::= Unsubscribe(self, topic, subHops - 1, mid)
    membershipActor ! GetNeighbors
  }

  def publish(topic: String, m: String) = {
    val mid = Utils.md5("PUB" + topic + self + m + Utils.getDate)

    delivered += mid
    pendingPub ::= Publish(topic, pubHops - 1, m, mid)
    membershipActor ! GetNeighbors
  }


  override def receive = {
    case Subscribe(subscriber, topic, dateTTL, hops, mid) =>
      receiveSub(Subscribe(subscriber, topic, dateTTL, hops, mid))

    case Unsubscribe(unsubscriber, topic, hops, mid) =>
      receiveUnsub(Unsubscribe(unsubscriber, topic, hops, mid))

    case Publish(topic, hops, message, mid) =>
      receivePub(Publish(topic, hops, message, mid))

    case DirectMessage(topic, message, mid) =>
      receiveDirectMsg(DirectMessage(topic, message, mid))

    case Neighbors(newNeighbors) =>
      receiveNeighbors(newNeighbors)
  }


  def receiveSub(subscribe: Subscribe) = { // TODO deliver my own message?
    if (!delivered.contains(subscribe.mid)) {

      radiusSubsByTopic += (subscribe.topic -> (subscribe.subscriber, subscribe.dateTTL))
      delivered += subscribe.mid

      if (subscribe.subHops > 0) {
        pendingSub ::= subscribe.copy(subHops = subscribe.subHops - 1)
        membershipActor ! GetNeighbors
      }

    }

  }

  def receiveUnsub(unsubscribe: Unsubscribe) = {
    if (!delivered.contains(unsubscribe.mid)) {

      //      radiusSubsByTopic = radiusSubsByTopic.filter(p => p._1.equals(unsubscribe.topic)) // TODO remove based on value

      delivered += unsubscribe.mid

      if (unsubscribe.unsubHops > 0) {
        pendingUnsub ::= unsubscribe.copy(unsubHops = unsubscribe.unsubHops - 1)
        membershipActor ! GetNeighbors
      }

    }

  }

  def receivePub(publish: Publish) = {
    if (!delivered.contains(publish.mid)) {
      delivered += publish.mid

      val dateTTLOpt = mySubs.get(publish.topic)
      if (dateTTLOpt.isDefined && dateTTLOpt.get.after(Utils.getDate))
        testAppActor ! PSDelivery(publish.topic, publish.message)


      radiusSubsByTopic(publish.topic)
        .filter(p => p._2.after(Utils.getDate))
        .foreach(p => p._1 ! DirectMessage(publish.topic, publish.message, publish.mid))

      if (publish.pubHops > 0) {
        pendingPub ::= publish.copy(pubHops = publish.pubHops - 1)
        membershipActor ! GetNeighbors
      }

    }

  }


  def receiveDirectMsg(directMessage: DirectMessage) = {

    if (!delivered.contains(directMessage.mid)) {
      delivered += directMessage.mid

      val dateTTLOpt = mySubs.get(directMessage.topic)
      if (dateTTLOpt.isDefined && dateTTLOpt.get.after(Utils.getDate)) //necessário fazer verificação do TTL?
        testAppActor ! PSDelivery(directMessage.topic, directMessage.message)

    }

  }

  def receiveNeighbors(newNeighbors: List[ActorRef]) = {
    neighbors = newNeighbors

    pendingSub.foreach { sub =>
      membershipActor ! Gossip(sub)
    }

    pendingUnsub.foreach { unsub =>
      membershipActor ! Gossip(unsub)
    }

    pendingPub.foreach { pub =>
      membershipActor ! Gossip(pub)
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
