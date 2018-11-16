import akka.actor.{ActorSystem, Props}
import akka.actor.{ActorLogging, ActorSystem, PoisonPill, Props}
import akka.event.Logging
import gossip.GossipActor
import membership.HyParViewActor
import pubsub.{PubSubActor, Publish, Subscribe, Unsubscribe}
import testapp.TestAppActor
import utils.{Node, Start}


object Main extends App {
  override def main(args: Array[String]) = {
    val a = ActorSystem("nodeAsystem")
    val b = ActorSystem("nodeBSystem")
    val c = ActorSystem("nodeCSytstem")

    val aHyParView = a.actorOf(Props[HyParViewActor], "aHyParView")
    val bHyParView = b.actorOf(Props[HyParViewActor], "bHyParView")
    val cHyParView = c.actorOf(Props[HyParViewActor], "cHyParView")

    val aNode = Node("aNode", null, null, null, aHyParView)
    val bNode = Node("bNode", null, null, null, bHyParView)
    val cNode = Node("cNode", null, null, null, cHyParView)

    aHyParView ! membership.StartLocal(null, aNode)
    bHyParView ! membership.StartLocal(aNode, bNode)
    cHyParView ! membership.StartLocal(aNode, cNode)

  }
}