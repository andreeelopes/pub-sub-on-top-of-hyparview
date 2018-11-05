package pubsub

import java.util.Date

import akka.actor.{Actor, ActorRef}
import membership.GetNeighbors
import utils.Utils

class PubSubActor(n: Int, membershipActor: ActorRef) extends Actor {

  val diameter = math.log(n * 10)
  var radiusSubsByTopic = Map[String, Set[(ActorRef, Date)]]()
  var radiusSubsByProcess = Map[ActorRef, Set[(String, Date)]]()
  val subHops = Int((diameter + 1) / 2)
  val pubHops = Int((diameter + 1) / 2)
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
    val mid = Utils.md5(topic + self + dateTTL)
    delivered ::= mid
    pendingSub ::= Subscribe(self, topic, dateTTL, subHops, mid)
    membershipActor ! GetNeighbors
  }

  def unsubscribe(topic: String) = {
    removeFromRadiusSubs(topic, self)
    //  mid ← generateUID({UNSUB, myself, topic, getTimeOfSystem()})
    //  delivered ← delivered U {mid}
    //  pending ← pendingUnsub U {(UNSUB, myself, topic, subHops - 1, mid)}
    //  Trigger GetNeighbors ()
  }





  def addToRadiusSubs(topic: String, process: ActorRef, ttl: Date) = {
    radiusSubsByProcess += (process -> (topic, ttl))
    radiusSubsByTopic += (topic -> (process, ttl))
  }

  def removeFromRadiusSubs(topic: String, process: ActorRef) = {
    radiusSubsByTopic -= topic
    radiusSubsByProcess(process) = radiusSubsByProcess(process).filter(p => !p._1.equals(topic))
  }

  override def receive = ???

}
