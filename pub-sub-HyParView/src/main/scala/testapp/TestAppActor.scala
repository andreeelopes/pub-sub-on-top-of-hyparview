package testapp


import akka.actor.{Actor, ActorLogging}
import pubsub.{PSDelivery, Publish, Subscribe}
import utils.{Node, Start}
import java.io.{BufferedWriter, File, FileWriter, PrintWriter}

import scala.util.Random


class TestAppActor extends Actor with ActorLogging {

  val numberOfTopics = 50
  val subscribeN = 5

  var myNode: Node = _

  var publishCount = Map[String, Int]()
  var receivedCount = Map[String, Int]()

  var suicideAfter = 60

  var myTopics = List[Int]()

  val randomTopics = Random.shuffle(generateList(numberOfTopics)).take(subscribeN)

  populateMaps()


  override def receive = {
    case Start(node) =>
      log.info("Starting testApp: " + node)

      myNode = node

      for (i <- 0 until subscribeN) {
        val topic = s"${randomTopics(i)}"
        myTopics ::= topic.toInt
        log.info(s"Subscribing -> $topic")
        myNode.pubSubActor ! Subscribe(topic)
      }

      Thread.sleep(5000)

      for (i <- 1 to suicideAfter) {
        Thread.sleep(1000)
        val topic = Random.nextInt(numberOfTopics - 1)
        log.info(s"Publishing -> $topic")
        publishCount = incrementCount(topic.toString, publishCount)
        myNode.pubSubActor ! Publish(s"$topic", s"${myNode.name}:$i")
      }


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

    mapAux
  }


  def printStats() = {

    val file = new File(s"${myNode.name}.txt")
    val pw = new BufferedWriter(new FileWriter(file))

    //    pw.write("Subscribed")
    myTopics.foreach(t => println(s"1,$t,-1"))

    //    pw.write("Published")
    publishCount.foreach(p => println(s"2,${p._1},${p._2}"))

    //    pw.write("Received")
    receivedCount.foreach(p => println(s"3,${p._1},${p._2}"))

    pw.close()

  }


  def populateMaps() = {
    for (i <- 1 to numberOfTopics) {
      publishCount += (i.toString -> 0)
      receivedCount += (i.toString -> 0)
    }
  }


  def generateList(numberOfTopics: Int) = {
    var list = List[Int]()
    for (i <- 0 until numberOfTopics)
      list ::= i
    list
  }


}
