
import akka.actor.{ActorSystem, Props}
import gossip.GossipActor
import membership.HyParViewActor
import pubsub.{PubSubActor, Publish, Subscribe}
import testapp.TestAppActor
import utils.{Node, Start}

object Main {
  def main(args: Array[String]): Unit = {
    val andre = ActorSystem("andresystem")
    val nelson = ActorSystem("nelsonsystem")
    val simon = ActorSystem("simonsystem")

    val testAppA = andre.actorOf(Props[TestAppActor], "andretestapp")
    val pubsubA = andre.actorOf(Props(new PubSubActor(2)), "andrepubsub")
    val gossipA = andre.actorOf(Props(new GossipActor(3)), "andregossip")
    val membershipA = andre.actorOf(Props[HyParViewActor], "andremembership")

    val testAppN = nelson.actorOf(Props[TestAppActor], "nelsontestapp")
    val pubsubN = nelson.actorOf(Props(new PubSubActor(2)), "nelsonpubsub")
    val gossipN = andre.actorOf(Props(new GossipActor(3)), "nelsongossip")
    val membershipN = nelson.actorOf(Props[HyParViewActor], "nelsonmembership")

    val testAppS = nelson.actorOf(Props[TestAppActor], "simontestapp")
    val pubsubS = simon.actorOf(Props(new PubSubActor(2)), "simonpubsub")
    val gossipS = andre.actorOf(Props(new GossipActor(3)), "simongossip")
    val membershipS = simon.actorOf(Props[HyParViewActor], "simonmembership")


    //Starting Andre System
    val andreNode = Node("andre", testAppA, pubsubA, gossipA, membershipA)

    testAppA ! Start(andreNode)
    pubsubA ! Start(andreNode)
    gossipA ! Start(andreNode)
    membershipA ! membership.Start(null, andreNode)

    //Starting Nelson System
    Thread.sleep(1000)

    val nelsonNode = Node("nelson", testAppN, pubsubN, gossipN, membershipN)

    testAppN ! Start(nelsonNode)
    pubsubN ! Start(nelsonNode)
    gossipN ! Start(nelsonNode)
    membershipN ! membership.Start(andreNode, nelsonNode)

    //Starting Simon System
    Thread.sleep(1000)

    val simonNode = Node("simon", testAppS, pubsubS, gossipS, membershipS)

    testAppS ! Start(simonNode)
    pubsubS ! Start(simonNode)
    gossipS ! Start(simonNode)
    membershipS ! membership.Start(nelsonNode, simonNode)

    Thread.sleep(1000)
    println("\n\n ---Andre subscribing futebol--- \n\n")
    testAppA ! Subscribe("futebol")

    Thread.sleep(1000)
    println("\n\n ---Nelson publishing futebol--- \n\n")
    testAppN ! Publish("futebol", "o bruno de carvalho é uma besta")


  }


}