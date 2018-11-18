package testapp


import akka.actor.{Actor, ActorLogging, PoisonPill}
import pubsub.{PSDelivery, Publish, Subscribe}
import utils.{Node, Start}
import java.io.{BufferedWriter, File, FileWriter, PrintWriter}
import java.util.concurrent.TimeUnit

import membership.{CheckMetricsReceived, MetricsDelivery, MetricsRequest}

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, FiniteDuration}


class TestAppActor extends Actor with ActorLogging {

  var numberOfMetricsMessagesReceived = 0
  var metrics: Map[String, List[Int]] = Map[String, List[Int]]()
  var wrote = false

  val numberOfTopics = 50
  val subscribeN = 5

  var myNode: Node = _

  var publishCount = Map[String, Int]()
  var receivedCount = Map[String, Int]()

  var suicideAfter = 10

  var myTopics = List[Int]()

  val randomTopics = Random.shuffle(generateList(numberOfTopics)).take(subscribeN)

  context.system.scheduler.schedule(FiniteDuration(1, TimeUnit.SECONDS),
    Duration(1, TimeUnit.SECONDS), self, CheckMetricsReceived)

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

      for (i <- 0 to suicideAfter) {
        Thread.sleep(1000)
        val topic = Random.nextInt(numberOfTopics)
        log.info(s"Publishing -> $topic")
        publishCount = incrementCount(topic.toString, publishCount)
        myNode.pubSubActor ! Publish(s"$topic", s"${myNode.name}:$i")
      }

      Thread.sleep(1000)

      myNode.gossipActor ! MetricsRequest
      myNode.membershipActor ! MetricsRequest
      myNode.pubSubActor ! MetricsRequest


    case PSDelivery(topic, m) =>
      receivedCount = incrementCount(topic, receivedCount)
      log.info(s"Received -> ($topic):$m")

    case StatsAndDie =>
      printStats()

    case MetricsDelivery(layer, outgoingMessages, incomingMessages) =>
      metrics += (layer -> List(outgoingMessages, incomingMessages))
      numberOfMetricsMessagesReceived += 1

    case CheckMetricsReceived =>
      processMetricsReceived()

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


  def printStats(): Unit = {

    val file = new File(s"../rawoutput/${myNode.name}.csv")
    val pw = new BufferedWriter(new FileWriter(file))

    //pw.write("Subscribed")
    myTopics.foreach(t => pw.write(s"$myNode,1,$t,-1\n"))

    //pw.write("Published")
    publishCount.foreach(p => pw.write(s"$myNode,2,${p._1},${p._2}\n"))

    //pw.write("Received")
    receivedCount.foreach(p => pw.write(s"$myNode,3,${p._1},${p._2}\n"))


    pw.close()
  }

  def processMetricsReceived(): Unit = {
    if (numberOfMetricsMessagesReceived == 3 && !wrote) {

      val file = new File(s"../rawoutput/${myNode.name}-outin.txt")
      val pw = new BufferedWriter(new FileWriter(file))

      metrics.foreach(pair => pw.write(s"$myNode,4,${pair._1},${pair._2.head},${pair._2(1)}\n"))

      wrote = true
      pw.close()
    }
  }


  def populateMaps() = {
    for (i <- 0 until numberOfTopics) {
      publishCount += (i.toString -> 0)
      receivedCount += (i.toString -> 0)
    }
  }


  def generateList(numberOfTopics: Int) = {
    var list = List[Int]()
    for (i <- 0 until numberOfTopics) {
      list ::= i
    }
    list
  }


}
