
import akka.actor.{ActorSystem, Props}
import gossip.{GossipActor}
import membership.HyParViewActor
import pubsub.{PubSubActor, Subscribe}
import testapp.TestAppActor

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
    testAppA ! testapp.Start(pubsubA)
    pubsubA ! pubsub.Start(gossipA, testAppA)
    gossipA ! gossip.Start(membershipA, pubsubA)
    membershipA ! membership.Start(null, gossipA)

    //Starting Nelson System
    testAppN ! testapp.Start(pubsubN)
    pubsubN ! pubsub.Start(gossipN, testAppN)
    gossipN ! gossip.Start(membershipN, pubsubN)
    membershipN ! membership.Start(membershipA, gossipN)

    //Starting Simon System
    testAppS ! testapp.Start(pubsubS)
    pubsubS ! pubsub.Start(gossipS, testAppS)
    gossipS ! gossip.Start(membershipS, pubsubS)
    membershipS ! membership.Start(membershipN, gossipS)

    Thread.sleep(1000)

    testAppA ! Subscribe("futebol")


  }


}