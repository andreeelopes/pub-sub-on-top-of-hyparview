package testapp

import java.io.File

import akka.actor.{Actor, ActorLogging}
import pubsub.{PSDelivery, Publish, Subscribe}
import utils.{Node, Start}
import java.io.PrintWriter

import scala.util.Random


class TestAppActor extends Actor with ActorLogging {

  val numberOfTopics = 50
  val subscribeN = 5

  var myNode: Node = _

  var publishCount = Map[String, Int]()
  var receivedCount = Map[String, Int]()

  var suicideAfter = 1

  val randomTopics = Random.shuffle(List(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)).take(subscribeN)


  override def receive = {
    case Start(node) =>
      log.info("Starting testApp: " + node)

      myNode = node

      for (i <- 1 to subscribeN) {
        val topic = s"T${randomTopics(i - 1)}"

        log.info(s"Subscribing -> $topic")
        publishCount = incrementCount(topic, publishCount)
        myNode.pubSubActor ! Subscribe(topic)
      }

      Thread.sleep(5000)

      for (i <- 1 to suicideAfter) {
        Thread.sleep(1000)
        val topic = Random.nextInt(numberOfTopics)
        log.info(s"Publishing -> T$topic")
        myNode.pubSubActor ! Publish(s"T$topic", s"${myNode.name}:$i")
      }

      Thread.sleep(5000)

    case PSDelivery(topic, m) =>
      receivedCount = incrementCount(topic, receivedCount)
      log.info(s"Received -> ($topic):$m")

    case StatsAndDie =>
      printStats()

  }

  def incrementCount(topic: String, map: Map[String, Int]) = {
    var mapAux = map

    val countOpt = map.get(topic)

    if (countOpt.isDefined) {
      val count = countOpt.get + 1
      mapAux += (topic -> count)
    }
    else
      mapAux += (topic -> 1)

    mapAux
  }


  def printStats() = {

    new PrintWriter(myNode.name) {

      write("Subscribed")
      for (i <- randomTopics.indices) write(randomTopics(i).toString)

      write("Published")
      publishCount.foreach(p => write(s"${p._1}:${p._2}"))

      write("Received")
      receivedCount.foreach(p => write(s"${p._1}:${p._2}"))

      close()
    }


  }


}
