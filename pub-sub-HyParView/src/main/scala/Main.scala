import java.util.Date

import akka.actor.{ActorRef, ActorSystem, Props}
import membership.{Gossip, HyParViewActor}
import pubsub.{PassSubscribe, PubSubActor, Subscribe}
import testapp.TestAppActor

object Main {
  def main(args: Array[String]): Unit = {
    val andre = ActorSystem("andresystem")
    val nelson = ActorSystem("nelsonsystem")
    val simon = ActorSystem("simonsystem")

    val pubsubA = andre.actorOf(Props(new PubSubActor(2)), "andrepubsub")
    val membershipA = andre.actorOf(Props[HyParViewActor], "andremembership")
    val testAppA = andre.actorOf(Props[TestAppActor], "andretestapp")

    val pubsubN = nelson.actorOf(Props(new PubSubActor(2)), "nelsonpubsub")
    val membershipN = nelson.actorOf(Props[HyParViewActor], "nelsonmembership")
    val testAppN = nelson.actorOf(Props[TestAppActor], "nelsontestapp")

    val pubsubS = simon.actorOf(Props(new PubSubActor(2)), "simonpubsub")
    val membershipS = simon.actorOf(Props[HyParViewActor], "simonmembership")
    val testAppS = nelson.actorOf(Props[TestAppActor], "simontestapp")


    //Starting Andre System
    pubsubA ! pubsub.Start(membershipA, testAppA)
    membershipA ! membership.Start(null, pubsubA)
    testAppA ! testapp.Start(pubsubA)

    //Starting Nelson System
    pubsubN ! pubsub.Start(membershipN, testAppN)
    membershipN ! membership.Start(membershipA, pubsubN)
    testAppN ! testapp.Start(pubsubN)

    //Starting Simon System
    pubsubS ! pubsub.Start(membershipS, testAppS)
    membershipS ! membership.Start(membershipN, pubsubS)
    testAppS ! testapp.Start(pubsubS)

    Thread.sleep(2000)

    testAppA ! Subscribe("futebol")

    //    membershipA ! Gossip("olá".getBytes(), PassSubscribe(null, "futebol", null, 10, "olá".getBytes()))


  }


}