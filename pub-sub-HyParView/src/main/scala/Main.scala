import akka.actor.{ActorSystem, Props}
import akka.actor.{ActorLogging, ActorSystem, PoisonPill, Props}
import akka.event.Logging
import gossip.GossipActor
import membership.HyParViewActor
import pubsub.{PubSubActor, Publish, Subscribe, Unsubscribe}
import testapp.TestAppActor
import utils.{Node, Start, Utils}


object Main extends App {
  override def main(args: Array[String]) = {
    val a = ActorSystem("nodeAsystem")
    val b = ActorSystem("nodeBSystem")
    val c = ActorSystem("nodeCSytstem")
    val d = ActorSystem("nodeDSystem")
    val e = ActorSystem("nodeDSystem")


    val aHyParView = a.actorOf(Props[HyParViewActor], "aHyParView")
    val bHyParView = b.actorOf(Props[HyParViewActor], "bHyParView")
    val cHyParView = c.actorOf(Props[HyParViewActor], "cHyParView")
    val dHyParView = d.actorOf(Props[HyParViewActor], "dHyParView")
    val eHyParView = e.actorOf(Props[HyParViewActor], "eHyParView")


    val aNode = Node("aNode", null, null, null, aHyParView)
    val bNode = Node("bNode", null, null, null, bHyParView)
    val cNode = Node("cNode", null, null, null, cHyParView)
    val dNode = Node("dNode", null, null, null, dHyParView)
    val eNode = Node("eNode", null, null, null, eHyParView)


    aHyParView ! membership.StartLocal(null, aNode)
    bHyParView ! membership.StartLocal(aNode, bNode)
    cHyParView ! membership.StartLocal(aNode, cNode)
    dHyParView ! membership.StartLocal(aNode, dNode)
    eHyParView ! membership.StartLocal(aNode, eNode)


    Thread.sleep(1000)
    println(s"--------------------------------------KILLING C at ${Utils.getDate}---------------------------------------")
    cHyParView ! PoisonPill

    Thread.sleep(6500)


    println(s"--------------------------------------KILLING at ${Utils.getDate}---------------------------------------")
    bHyParView ! PoisonPill


    Thread.sleep(6500)
    println(s"--------------------------------------KILLING A D at ${Utils.getDate}---------------------------------------")
    dHyParView ! PoisonPill
    aHyParView ! PoisonPill

  }
}