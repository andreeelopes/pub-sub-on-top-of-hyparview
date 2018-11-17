import akka.actor.{ActorSystem, PoisonPill, Props}
import com.typesafe.config.ConfigFactory
import gossip.GossipActor
import membership.HyParViewActor
import pubsub.PubSubActor
import testapp.{StatsAndDie, TestAppActor}
import utils.{Node, Start}


object Remote extends App {

  override def main(args: Array[String]) = {

    val ip = args(0)
    val port = args(1)

    val config = ConfigFactory.parseString(getConf(ip, port))

    val system = ActorSystem("RemoteService", config)

    val testAppActor = system.actorOf(Props[TestAppActor], "testApp")
    val pubSubActor = system.actorOf(Props(new PubSubActor(2)), "pubSub")
    val gossipActor = system.actorOf(Props(new GossipActor(3)), "gossip")
    val membershipActor = system.actorOf(Props[HyParViewActor], "membership")

    val node = Node(s"$ip:$port", testAppActor, pubSubActor, gossipActor, membershipActor)
    println("MyNode: " + node)

    var contactAkkaId: String = null

    try {
      val ipContactNode = args(2)
      val portContactNode = args(3)
      contactAkkaId = s"akka.tcp://RemoteService@$ipContactNode:$portContactNode/user/membership"
    } catch {
      case _: Exception =>
    }


    pubSubActor ! Start(node)
    gossipActor ! Start(node)
    membershipActor ! membership.Start(contactAkkaId, node)

    Thread.sleep(5 * 1000)

    testAppActor ! Start(node)

    Thread.sleep(3 * 60 * 1000)

    testAppActor ! StatsAndDie

    Thread.sleep(5000)

    system.terminate()


  }


  def getConf(ip: String, port: String) = {
    s"""
       |akka {
       |  event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
       |   loglevel = "DEBUG"
       |   actor {
       |     provider = "remote"
       |     warn-about-java-serializer-usage = false
       |   }
       |   remote {
       |     enabled-transports = ["akka.remote.netty.tcp"]
       |     netty.tcp {
       |       hostname = "$ip"
       |       port = $port
       |     }
       |   }
       |  }
    """.stripMargin
  }


}
