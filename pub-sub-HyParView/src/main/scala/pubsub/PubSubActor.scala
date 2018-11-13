package pubsub

import java.util.Date

import akka.actor.{Actor, ActorRef}
import membership.{GetNeighbors, Gossip, Neighbors}
import utils.Utils

class PubSubActor(n: Int, membershipActor: ActorRef, testAppActor: ActorRef) extends Actor {

  val diameter = math.log(n * 10)
  var radiusSubsByTopic = Map[String, Set[(ActorRef, Date)]]()
  var radiusSubsByProcess = Map[ActorRef, Set[(String, Date)]]()
  val subHops = ((diameter + 1) / 2).toInt
  val pubHops = ((diameter + 1) / 2).toInt
  var neighbors = List[ActorRef]()
  var delivered = List[Array[Byte]]()
  var pendingSub = List[Subscribe]()
  var pendingUnsub = List[Unsubscribe]()
  var pendingPub = List[Publish]()
  val TTL = 30 //s

  //  context.system.scheduler.schedule(interval = duration.Duration(Duration.Duration))


  def subscribe(topic: String) = {
    val dateTTL = Utils.getDatePlusTime(TTL)
    addToRadiusSubs(topic, self, dateTTL)
    val mid = Utils.md5("SUB" + topic + self + Utils.getDate)
    delivered ::= mid
    pendingSub ::= Subscribe(self, topic, dateTTL, subHops - 1, mid)
    membershipActor ! GetNeighbors
  }

  def unsubscribe(topic: String) = {
    removeFromRadiusSubs(topic, self)
    val mid = Utils.md5("UNSUB" + topic + self + Utils.getDate)
    delivered ::= mid
    pendingUnsub ::= Unsubscribe(self, topic, subHops - 1, mid)
    membershipActor ! GetNeighbors
  }

  def publish(topic: String, m: String) = {
    val mid = Utils.md5("PUB" + topic + self + m + Utils.getDate) //change pseudo-code
    delivered ::= mid
    pendingPub ::= Publish(topic, pubHops - 1, m, mid) //change pseudo-code
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

    case Neighbors(neighbors) =>
      receiveNeighbors(neighbors)
  }


  def receiveNeighbors(newNeighbors: List[ActorRef]) = {
    neighbors = newNeighbors

    pendingSub.foreach { s =>
      membershipActor ! Gossip()
    }

    pendingUnsub.foreach { u =>
      membershipActor ! Gossip()
    }

    pendingPub.foreach { p =>
      membershipActor ! Gossip()
    }

    pendingSub = List()
    pendingSub = List()
    pendingSub = List()

  }


  def receiveSub(subscribe: Subscribe) = { //TODO deliver my own message?
    if (!delivered.contains(subscribe.mid)) {

      addToRadiusSubs(subscribe.topic, subscribe.subscriber, subscribe.dateTTL)
      delivered ::= subscribe.mid

      if (subscribe.subHops > 0) {
        pendingSub ::= subscribe.copy(subHops = subscribe.subHops - 1)
        membershipActor ! GetNeighbors
      }

    }

  }

  def receiveUnsub(unsubscribe: Unsubscribe) = {
    if (!delivered.contains(unsubscribe.mid)) {
      removeFromRadiusSubs(unsubscribe.topic, unsubscribe.unsubscriber)
      delivered ::= unsubscribe.mid

      if (unsubscribe.unsubHops > 0) {
        pendingUnsub ::= unsubscribe.copy(unsubHops = unsubscribe.unsubHops - 1)
        membershipActor ! GetNeighbors
      }

    }

  }

  def receivePub(publish: Publish) = {
    if (!delivered.contains(publish.mid)) {
      delivered ::= publish.mid

      radiusSubsByTopic(publish.topic)
        //.filter(p => p._2 > 0 && p._1.equals(self))
        .foreach(p => testAppActor ! PSDelivery(publish.topic, publish.message))

      radiusSubsByTopic(publish.topic)
        //.filter(p => p._2 > 0 && !p._1.equals(self))
        .foreach(p => p._1 ! DirectMessage(publish.topic, publish.message, publish.mid))

      if (publish.pubHops > 0) {
        pendingPub ::= publish.copy(pubHops = publish.pubHops - 1)
        membershipActor ! GetNeighbors
      }

    }


  }

  //radiusSubsByProcess it is not necessary
  def receiveDirectMsg(directMessage: DirectMessage) = {

    if (!delivered.contains(directMessage.mid)) {
      delivered ::= directMessage.mid
      radiusSubsByTopic(directMessage.topic)
        //        .filter(p=> p._2 > 0 && p._1.equals(self))
        .foreach(p => testAppActor ! PSDelivery(directMessage.topic, directMessage.message))
    }

  }

  def renewSub() = {
    radiusSubsByProcess(self)
      //      .filter( e =>  e._2 < TTL * 0.2)
      .foreach(e => subscribe(e._1))
  }


  def cleanOldSubs() = {
    radiusSubsByProcess.foreach { p =>
      p._2.foreach { v =>
        if (true /*ttl<0*/ )
          removeFromRadiusSubs(v._1, p._1)
      }
    }
  }


  def addToRadiusSubs(topic: String, process: ActorRef, ttl: Date) = {
    radiusSubsByProcess += (process -> (topic, ttl))
    radiusSubsByTopic += (topic -> (process, ttl))
  }

  def removeFromRadiusSubs(topic: String, process: ActorRef) = {
    radiusSubsByTopic -= topic
    radiusSubsByProcess(process) = radiusSubsByProcess(process).filter(p => !p._1.equals(topic))
  }


}
